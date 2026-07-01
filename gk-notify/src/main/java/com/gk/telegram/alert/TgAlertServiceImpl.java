package com.gk.telegram.alert;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.telegram.TgAlertService;
import com.gk.infra.utils.AsynUtils;
import com.gk.telegram.dao.TgChatDao;
import com.gk.telegram.dao.TgMessageTaskDao;
import com.gk.telegram.entity.TgChatEntity;
import com.gk.telegram.entity.TgMessageTaskEntity;
import com.gk.telegram.support.TgConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Telegram 告警与通知任务创建服务。
 * <p>
 * 只负责找到应接收消息的 tg_chat，并创建 tg_message_task；
 * 真正调用 Telegram API 发送消息由 {@link TgMessageTaskExecutor} 异步执行。
 * <p>
 * 消息展示按 SaaS 层级控制：平台群展示租户和商户，租户群只展示商户，商户群不展示租户和商户。
 */
@Service("tgAlertBotService")
@Slf4j
@RequiredArgsConstructor
public class TgAlertServiceImpl implements TgAlertService {
    private final TgChatDao tgChatDao;
    private final TgMessageTaskDao tgMessageTaskDao;

    @Override
    public void sysError(String title, String content, String traceId) {
        sysError(0L, 0L, title, content, traceId);
    }

    @Override
    public void sysWarn(String title, String content, String traceId) {
        sysWarn(0L, 0L, title, content, traceId);
    }

    /** 创建系统错误告警任务。 */
    @Override
    public void sysError(Long tenantId, Long merchantId, String title, String content, String traceId) {
        createAlertAsync(TgAlertEventType.SYSTEM_ERROR, tenantId, merchantId, title, content, traceId);
    }

    /** 创建系统预警任务。 */
    @Override
    public void sysWarn(Long tenantId, Long merchantId, String title, String content, String traceId) {
        createAlertAsync(TgAlertEventType.SYSTEM_WARN, tenantId, merchantId, title, content, traceId);
    }

    /** 创建风控预警任务。 */
    @Override
    public void riskAlert(Long tenantId, Long merchantId, String title, String content, String traceId) {
        createAlertAsync(TgAlertEventType.RISK_ALERT, tenantId, merchantId, title, content, traceId);
    }

    /** 创建支付成功通知任务。 */
    @Override
    public void paySuccess(Long tenantId, Long merchantId, String title, String content, String traceId) {
        createAlertAsync(TgAlertEventType.PAYIN_SUCCESS, tenantId, merchantId, title, content, traceId);
    }

    /** 创建代付成功通知任务。 */
    @Override
    public void payoutSuccess(Long tenantId, Long merchantId, String title, String content, String traceId) {
        createAlertAsync(TgAlertEventType.PAYOUT_SUCCESS, tenantId, merchantId, title, content, traceId);
    }

    /** 创建代付失败通知任务。 */
    @Override
    public void payoutFailed(Long tenantId, Long merchantId, String title, String content, String traceId) {
        createAlertAsync(TgAlertEventType.PAYOUT_FAILED, tenantId, merchantId, title, content, traceId);
    }

    private void createAlertAsync(TgAlertEventType eventType, Long tenantId, Long merchantId,
                                 String title, String content, String traceId) {
        AsynUtils.execute("Telegram alert task create", () -> {
            int created = createAlert(eventType, tenantId, merchantId, title, content, traceId);
            log.debug("Telegram alert task created, eventType={}, title={}, created={}", eventType.code(), title, created);
        });
    }

    /**
     * 通用任务创建入口。
     * <p>
     * 同一事件可能投递到多个层级群；每个目标群单独渲染消息，避免展示不该看到的主体信息。
     *
     * @return 成功创建的 tg_message_task 数量
     */
    private int createAlert(TgAlertEventType eventType, Long tenantId, Long merchantId,
                            String title, String content, String traceId) {
        List<TgChatEntity> targets = findTargets(eventType, tenantId, merchantId);
        int created = 0;
        for (TgChatEntity target : targets) {
            if (!subscribes(target, eventType)) {
                continue;
            }
            String text = renderText(eventType, title, content);
            TgMessageTaskEntity task = buildTask(target, eventType, text, title, traceId);
            try {
                tgMessageTaskDao.insert(task);
                created++;
            } catch (DuplicateKeyException ignored) {
                // 同一个群同一内容已存在任务时跳过，依赖 uk_tg_msg_idem 防重复刷屏。
            }
        }
        return created;
    }

    private String renderText(TgAlertEventType eventType, String title, String content) {
        return "<code>" + StringUtils.defaultIfBlank(title, resolveDefaultTitle(eventType)) + "</code>" + content;
    }

    /**
     * 查找事件对应的目标群。
     * <p>
     * 系统异常/系统预警只通知平台 OPS 群；风控告警可通知平台、租户、商户 OPS 群；
     * 业务通知只在当前租户范围内寻找 NOTIFY 群。
     */
    private List<TgChatEntity> findTargets(TgAlertEventType eventType, Long tenantId, Long merchantId) {
        QueryWrapper<TgChatEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .eq("purpose", resolvePurpose(eventType))
                .orderByAsc("id");

        if (isSystemAlertEvent(eventType)) {
            appendPlatformTarget(wrapper);
            wrapper.eq("event_types", eventType);
            return tgChatDao.selectList(wrapper);
        }

        if (isRiskAlertEvent(eventType)) {
            wrapper.and(scope -> {
                appendPlatformTarget(scope);
                if (!isEmptyId(tenantId)) {
                    scope.or(tenantScope -> appendTenantTarget(tenantScope, tenantId));
                    if (!isEmptyId(merchantId)) {
                        scope.or(merchantScope -> appendMerchantTarget(merchantScope, tenantId, merchantId));
                    }
                }
            });
            return tgChatDao.selectList(wrapper);
        }

        if (isEmptyId(tenantId)) {
            return List.of();
        }

        wrapper.and(scope -> {
            appendTenantTarget(scope, tenantId);
            if (!isEmptyId(merchantId)) {
                scope.or(merchantScope -> appendMerchantTarget(merchantScope, tenantId, merchantId));
            }
        });

        return tgChatDao.selectList(wrapper);
    }

    private void appendPlatformTarget(QueryWrapper<TgChatEntity> wrapper) {
        wrapper.and(scope -> scope.isNull("tenant_id").or().eq("tenant_id", 0L))
                .and(scope -> scope.isNull("merchant_id").or().eq("merchant_id", 0L));
    }

    private void appendTenantTarget(QueryWrapper<TgChatEntity> wrapper, Long tenantId) {
        wrapper.eq("tenant_id", tenantId)
                .and(scope -> scope.isNull("merchant_id").or().eq("merchant_id", 0L));
    }

    private void appendMerchantTarget(QueryWrapper<TgChatEntity> wrapper, Long tenantId, Long merchantId) {
        wrapper.eq("tenant_id", tenantId)
                .eq("merchant_id", merchantId);
    }

    private boolean isEmptyId(Long id) {
        return id == null || id == 0L;
    }

    /** 根据事件类型决定匹配 tg_chat.purpose 的用途。 */
    String resolvePurpose(TgAlertEventType eventType) {
        return switch (eventType) {
            case SYSTEM_ERROR, SYSTEM_WARN, RISK_ALERT -> TgConstants.ChatPurpose.OPS;
            case PAYIN_SUCCESS, PAYOUT_SUCCESS, PAYOUT_FAILED -> TgConstants.ChatPurpose.NOTIFY;
        };
    }

    /** 根据事件类型决定 tg_message_task.biz_type。 */
    private String resolveBizType(TgAlertEventType eventType) {
        return switch (eventType) {
            case SYSTEM_ERROR, SYSTEM_WARN, RISK_ALERT -> TgConstants.MessageBizType.SYSTEM_ALERT;
            case PAYIN_SUCCESS, PAYOUT_SUCCESS, PAYOUT_FAILED -> TgConstants.MessageBizType.BUSINESS_NOTIFY;
        };
    }

    /** 判断事件是否属于系统级告警。 */
    private boolean isSystemAlertEvent(TgAlertEventType eventType) {
        return switch (eventType) {
            case SYSTEM_ERROR, SYSTEM_WARN -> true;
            case RISK_ALERT, PAYIN_SUCCESS, PAYOUT_SUCCESS, PAYOUT_FAILED -> false;
        };
    }

    /** 判断事件是否属于风控告警。 */
    private boolean isRiskAlertEvent(TgAlertEventType eventType) {
        return TgAlertEventType.RISK_ALERT.equals(eventType);
    }

    /**
     * 判断群是否订阅了指定事件。
     * <p>tg_chat.event_types 为空代表接收全部事件；非空时按英文逗号分隔匹配。</p>
     */
    private boolean subscribes(TgChatEntity chat, TgAlertEventType eventType) {
        String eventTypes = chat.getEventTypes();
        if (StringUtils.isBlank(eventTypes)) {
            return true;
        }
        String expected = eventType.code();
        for (String item : eventTypes.split(",")) {
            if (expected.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 构建待发送消息任务。 */
    private TgMessageTaskEntity buildTask(TgChatEntity target, TgAlertEventType eventType,
                                          String text, String title, String traceId) {
        TgMessageTaskEntity task = new TgMessageTaskEntity();
        task.setTenantId(target.getTenantId());
        task.setMerchantId(target.getMerchantId());
        task.setBotId(target.getBotId());
        task.setChatId(target.getChatId());
        task.setTaskNo(BizKeyUtils.genTgMessageTaskNo());
        task.setBizType(resolveBizType(eventType));
        task.setBizNo(StringUtils.abbreviate(StringUtils.defaultIfBlank(title, eventType.code()), 128));
        task.setEventType(eventType.code());
        task.setSourceEventId(sourceEventId(eventType, target, title, traceId, text));
        task.setParseMode(TgConstants.ParseMode.HTML);
        task.setPayloadJson(JSON.toJSONString(Map.of("text", text)));
        task.setPayloadHash(SecureUtil.sha256(task.getPayloadJson()));
        task.setStatus("INIT");
        task.setRetryCount(0);
        task.setMaxRetryCount(8);
        task.setNextRetryAt(Instant.now());
        task.setTraceId(traceId);
        return task;
    }

    /** 生成来源事件标识；同一事件发到不同群时 chatId 不同，因此标识也不同。 */
    private String sourceEventId(TgAlertEventType eventType, TgChatEntity target, String title, String traceId, String text) {
        String seed = eventType.code() + "|" + target.getBotId() + "|" + target.getChatId() + "|"
                + StringUtils.defaultString(traceId) + "|" + StringUtils.defaultString(title) + "|" + text;
        return SecureUtil.sha256(seed).substring(0, 32);
    }

    private String resolveDefaultTitle(TgAlertEventType eventType) {
        return switch (eventType) {
            case SYSTEM_ERROR -> "系统异常提醒";
            case SYSTEM_WARN -> "系统警告提醒";
            case RISK_ALERT -> "风控提醒";
            case PAYIN_SUCCESS -> "代收成功通知";
            case PAYOUT_SUCCESS -> "代付成功通知";
            case PAYOUT_FAILED -> "代付失败通知";
        };
    }
}
