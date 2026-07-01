package com.gk.telegram.alert;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.telegram.TgBotService;
import com.gk.telegram.dao.TgChatDao;
import com.gk.telegram.dao.TgMessageTaskDao;
import com.gk.telegram.entity.TgChatEntity;
import com.gk.telegram.entity.TgMessageTaskEntity;
import com.gk.telegram.support.TgConstants;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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
@Service
@RequiredArgsConstructor
public class TgBotServiceImpl implements TgBotService {
    /** 消息中展示的本地时间格式。 */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final TgChatDao tgChatDao;
    private final TgMessageTaskDao tgMessageTaskDao;

    /** 创建系统错误告警任务。 */
    @Override
    public int sysError(Long tenantId, Long merchantId, String title, String content, String traceId) {
        return createAlert(TgAlertEventType.SYSTEM_ERROR, tenantId, merchantId, title, content, traceId);
    }

    /** 创建系统预警任务。 */
    @Override
    public int sysWarn(Long tenantId, Long merchantId, String title, String content, String traceId) {
        return createAlert(TgAlertEventType.SYSTEM_WARN, tenantId, merchantId, title, content, traceId);
    }

    /** 创建风控预警任务。 */
    @Override
    public int riskAlert(Long tenantId, Long merchantId, String title, String content, String traceId) {
        return createAlert(TgAlertEventType.RISK_ALERT, tenantId, merchantId, title, content, traceId);
    }

    /** 创建支付成功通知任务。 */
    @Override
    public int paySuccess(Long tenantId, Long merchantId, String title, String content, String traceId) {
        return createAlert(TgAlertEventType.PAYIN_SUCCESS, tenantId, merchantId, title, content, traceId);
    }

    /** 创建代付成功通知任务。 */
    @Override
    public int payoutSuccess(Long tenantId, Long merchantId, String title, String content, String traceId) {
        return createAlert(TgAlertEventType.PAYOUT_SUCCESS, tenantId, merchantId, title, content, traceId);
    }

    /** 创建代付失败通知任务。 */
    @Override
    public int payoutFailed(Long tenantId, Long merchantId, String title, String content, String traceId) {
        return createAlert(TgAlertEventType.PAYOUT_FAILED, tenantId, merchantId, title, content, traceId);
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
            String text = renderText(eventType, target, tenantId, merchantId, title, content, traceId);
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
        if (tenantId == null) {
            wrapper.isNull("tenant_id").isNull("merchant_id");
        } else if (isSystemAlertEvent(eventType)) {
            wrapper.isNull("tenant_id").isNull("merchant_id");
        } else if (isRiskAlertEvent(eventType)) {
            wrapper.and(w -> {
                w.nested(s -> s.isNull("tenant_id").isNull("merchant_id"))
                        .or(s -> s.eq("tenant_id", tenantId).isNull("merchant_id"));
                if (merchantId != null) {
                    w.or(s -> s.eq("tenant_id", tenantId).eq("merchant_id", merchantId));
                }
            });
        } else {
            wrapper.eq("tenant_id", tenantId);
            if (merchantId == null) {
                wrapper.isNull("merchant_id");
            } else {
                wrapper.and(w -> w.isNull("merchant_id").or().eq("merchant_id", merchantId));
            }
        }
        return tgChatDao.selectList(wrapper);
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

    /** 渲染 Telegram HTML 消息内容。 */
    private String renderText(TgAlertEventType eventType, TgChatEntity target, Long tenantId, Long merchantId,
                              String title, String content, String traceId) {
        Map<String, String> lines = new LinkedHashMap<>();
        appendVisibleSubjectLines(lines, target, tenantId, merchantId);
        if (StringUtils.isNotBlank(traceId)) {
            lines.put("TraceId", traceId);
        }
        lines.put("时间", TIME_FORMATTER.format(Instant.now()));

        StringBuilder sb = new StringBuilder();
        sb.append(TgHtml.bold("[" + eventType.code() + "] "
                + StringUtils.defaultIfBlank(title, eventType.code())));
        for (Map.Entry<String, String> entry : lines.entrySet()) {
            sb.append("\n").append(entry.getKey()).append(": ").append(TgHtml.code(entry.getValue()));
        }
        if (StringUtils.isNotBlank(content)) {
            sb.append("\n详情: ").append(TgHtml.escape(StringUtils.abbreviate(content, 1500)));
        }
        return sb.toString();
    }

    /**
     * 按接收群层级追加可见主体信息。
     * <p>平台群展示租户和商户；租户群只展示商户；商户群不展示租户和商户。</p>
     */
    private void appendVisibleSubjectLines(Map<String, String> lines, TgChatEntity target,
                                           Long tenantId, Long merchantId) {
        if (isPlatformTarget(target)) {
            lines.put("租户", tenantId == null ? "平台" : String.valueOf(tenantId));
            if (merchantId != null) {
                lines.put("商户", String.valueOf(merchantId));
            }
            return;
        }
        if (isTenantTarget(target) && merchantId != null) {
            lines.put("商户", String.valueOf(merchantId));
        }
    }

    /** 是否为平台群：tenant_id 和 merchant_id 都为空。 */
    private boolean isPlatformTarget(TgChatEntity target) {
        return target.getTenantId() == null && target.getMerchantId() == null;
    }

    /** 是否为租户群：tenant_id 有值，merchant_id 为空。 */
    private boolean isTenantTarget(TgChatEntity target) {
        return target.getTenantId() != null && target.getMerchantId() == null;
    }
}
