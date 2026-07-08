package com.gk.telegram.alert;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.infra.telegram.TgAlertEventType;
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
 * 这里只负责找到应该接收消息的 tg_chat，并创建 tg_message_task；
 * 真正调用 Telegram Bot API 发送消息由 {@link TgMessageTaskExecutor} 异步执行。
 */
@Service("tgAlertBotService")
@Slf4j
@RequiredArgsConstructor
public class TgAlertServiceImpl implements TgAlertService {
    private final TgChatDao tgChatDao;
    private final TgMessageTaskDao tgMessageTaskDao;

    @Override
    public void notify(TgAlertEventType eventType, Long tenantId, Long merchantId,
                       String title, String content, String traceId) {
        if (eventType == null) {
            log.warn("Skip Telegram alert task create, eventType is null, title={}", title);
            return;
        }
        createAlertAsync(eventType, tenantId, merchantId, title, content, traceId);
    }

    @Override
    public void notifySync(TgAlertEventType eventType, Long tenantId, Long merchantId,
                           String title, String content, String traceId) {
        if (eventType == null) {
            log.warn("Skip Telegram alert task create sync, eventType is null, title={}", title);
            return;
        }
        try {
            int created = createAlert(eventType, tenantId, merchantId, title, content, traceId);
            log.debug("Telegram alert task created sync, eventType={}, title={}, created={}",
                    eventType.code(), title, created);
        } catch (Exception e) {
            log.warn("Telegram alert task create sync failed, eventType={}, title={}, error={}",
                    eventType.code(), title, e.getMessage(), e);
        }
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
            } catch (DuplicateKeyException ex) {
                // 同一群同一内容已有任务时跳过，依赖 uk_tg_msg_idem 防止重复刷屏。
                log.error("机器人通知异常: {}", ex.getMessage());
            }
        }
        return created;
    }

    private String renderText(TgAlertEventType eventType, String title, String content) {
        return "<code>" + StringUtils.defaultIfBlank(title, resolveDefaultTitle(eventType)) + "</code>\n" +
                "──────────────\n" + content;
    }

    /**
     * 根据事件和租户/商户范围查找接收群。
     * <p>
     * purpose 只作为后台展示分类，不参与投递过滤；真正是否接收由 status、租户/商户范围、
     * event_types 决定。这样一个商户群可以同时接收业务、风控、渠道、活动等多类通知。
     */
    private List<TgChatEntity> findTargets(TgAlertEventType eventType, Long tenantId, Long merchantId) {
        QueryWrapper<TgChatEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", StatusEnum.NORMAL.code()).orderByAsc("id");
        if (isSystemAlertEvent(eventType)) {
            appendPlatformTarget(wrapper);
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
        wrapper.eq("tenant_id", 0L).eq("merchant_id", 0L);
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

    private String resolveBizType(TgAlertEventType eventType) {
        return switch (eventType) {
            case SYSTEM_ERROR, SYSTEM_WARN, RISK_WARN -> TgConstants.MessageBizType.SYSTEM_ALERT;
            case PAYIN_NOTICE, PAYOUT_NOTICE, CHANNEL_NOTICE, MERCHANT_NOTICE, ORDER_NOTICE, PSP_NOTICE ->
                    TgConstants.MessageBizType.BUSINESS_NOTIFY;
        };
    }

    private boolean isSystemAlertEvent(TgAlertEventType eventType) {
        return switch (eventType) {
            case SYSTEM_ERROR, SYSTEM_WARN -> true;
            case RISK_WARN, PAYIN_NOTICE, PAYOUT_NOTICE, CHANNEL_NOTICE, MERCHANT_NOTICE, ORDER_NOTICE, PSP_NOTICE ->
                    false;
        };
    }

    private boolean isRiskAlertEvent(TgAlertEventType eventType) {
        return TgAlertEventType.RISK_WARN.equals(eventType);
    }

    /**
     * 判断群是否订阅了指定事件。
     * <p>
     * tg_chat.event_types 必须显式配置事件类型；为空时不接收任何通知，避免误发到未配置的群。
     * 多个事件类型按英文逗号分隔。
     * </p>
     */
    private boolean subscribes(TgChatEntity chat, TgAlertEventType eventType) {
        String eventTypes = chat.getEventTypes();
        if (StringUtils.isBlank(eventTypes)) {
            return false;
        }
        String expected = eventType.code();
        for (String item : eventTypes.split(",")) {
            if (expected.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }

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

    private String sourceEventId(TgAlertEventType eventType, TgChatEntity target, String title, String traceId, String text) {
        String seed = eventType.code() + "|" + target.getBotId() + "|" + target.getChatId() + "|"
                + StringUtils.defaultString(traceId) + "|" + StringUtils.defaultString(title) + "|" + text;
        return SecureUtil.sha256(seed).substring(0, 32);
    }

    private String resolveDefaultTitle(TgAlertEventType eventType) {
        return switch (eventType) {
            case SYSTEM_ERROR -> "系统异常提醒";
            case SYSTEM_WARN -> "系统警告提醒";
            case RISK_WARN -> "风控提醒";
            case PAYIN_NOTICE -> "代收通知";
            case PAYOUT_NOTICE -> "代付通知";
            case CHANNEL_NOTICE -> "渠道通知";
            case MERCHANT_NOTICE -> "商户通知";
            case ORDER_NOTICE -> "订单通知";
            case PSP_NOTICE -> "四方通知";
        };
    }
}
