package com.gk.telegram.alert;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.telegram.support.NotifyKeyUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.infra.notify.NotifyService;
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

/**
 * Telegram 告警与通知任务创建服务。
 * <p>
 * 这里只负责找到应该接收消息的 tg_chat，并创建 tg_message_task；
 * 真正调用 Telegram Bot API 发送消息由 {@link TgMessageTaskExecutor} 异步执行。
 */
@Service("tgAlertBotService")
@Slf4j
@RequiredArgsConstructor
public class TgAlertServiceImpl implements NotifyService {
    private final TgChatDao tgChatDao;
    private final TgMessageTaskDao tgMessageTaskDao;

    @Override
    public void notify(String eventType, Long tenantId, Long merchantId,
                       String content, String traceId) {
        notify(eventType, tenantId, merchantId, content, traceId, TgConstants.ParseMode.HTML);
    }

    @Override
    public void notify(String eventType, Long tenantId, Long merchantId,
                       String content, String traceId, String parseMode) {
        if (eventType == null) {
            log.warn("Skip Telegram alert task create, eventType is null");
            return;
        }
        createAlertAsync(eventType, tenantId, merchantId, content, traceId, parseMode);
    }

    @Override
    public void notifySync(String eventType, Long tenantId, Long merchantId,
                           String content, String traceId) {
        notifySync(eventType, tenantId, merchantId, content, traceId, TgConstants.ParseMode.HTML);
    }

    @Override
    public void notifySync(String eventType, Long tenantId, Long merchantId,
                           String content, String traceId, String parseMode) {
        if (eventType == null) {
            log.warn("Skip Telegram alert task create sync, eventType is null");
            return;
        }
        try {
            int created = createAlert(eventType, tenantId, merchantId, content, traceId, parseMode);
            log.debug("Telegram alert task created sync, eventType={}, created={}", eventType, created);
        } catch (Exception e) {
            log.warn("Telegram alert task create sync failed, eventType={}, error={}",
                    eventType, e.getMessage(), e);
        }
    }

    private void createAlertAsync(String eventType, Long tenantId, Long merchantId,
                                  String content, String traceId, String parseMode) {
        AsynUtils.execute("Telegram alert task create", () -> {
            int created = createAlert(eventType, tenantId, merchantId, content, traceId, parseMode);
            log.debug("Telegram alert task created, eventType={}, created={}", eventType, created);
        });
    }

    /**
     * 通用任务创建入口。
     *
     * @return 成功创建的 tg_message_task 数量
     */
    private int createAlert(String eventType, Long tenantId, Long merchantId,
                            String content, String traceId, String parseMode) {
        String normalizedParseMode = TgConstants.ParseMode.normalize(parseMode);
        List<TgChatEntity> targets = findTargets(eventType, tenantId, merchantId);
        int created = 0;
        for (TgChatEntity target : targets) {
            if (!subscribes(target, eventType)) {
                continue;
            }
            TgMessageTaskEntity task = buildTask(target, eventType, content, traceId, normalizedParseMode);
            try {
                tgMessageTaskDao.insert(task);
                created++;
            } catch (DuplicateKeyException ex) {
                // 同一群同一来源事件已有任务时跳过，依赖 source_event_id 幂等。
                log.error("机器人通知异常: {}", ex.getMessage());
            }
        }
        return created;
    }

    /**
     * 根据事件和租户/商户范围查找接收群。
     * <p>
     * purpose 只作为后台展示分类，不参与投递过滤；真正是否接收由 status、租户/商户范围、
     * event_types 决定。这样一个商户群可以同时接收业务、风控、渠道、活动等多类通知。
     */
    private List<TgChatEntity> findTargets(String eventType, Long tenantId, Long merchantId) {
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

    private String resolveBizType(String eventType) {
        if (TgNotifyEventCodes.SYSTEM_ERROR.equalsIgnoreCase(eventType)
                || TgNotifyEventCodes.SYSTEM_WARN.equalsIgnoreCase(eventType)
                || TgNotifyEventCodes.RISK_WARN.equalsIgnoreCase(eventType)) {
            return TgConstants.MessageBizType.SYSTEM_ALERT;
        }
        return TgConstants.MessageBizType.BUSINESS_NOTIFY;
    }

    private boolean isSystemAlertEvent(String eventType) {
        return TgNotifyEventCodes.SYSTEM_ERROR.equalsIgnoreCase(eventType)
                || TgNotifyEventCodes.SYSTEM_WARN.equalsIgnoreCase(eventType);
    }

    private boolean isRiskAlertEvent(String eventType) {
        return TgNotifyEventCodes.RISK_WARN.equalsIgnoreCase(eventType);
    }

    /**
     * 判断群是否订阅了指定事件。
     * <p>
     * tg_chat.event_types 必须显式配置事件类型；为空时不接收任何通知，避免误发到未配置的群。
     * 多个事件类型按英文逗号分隔。
     * </p>
     */
    private boolean subscribes(TgChatEntity chat, String eventType) {
        String eventTypes = chat.getEventTypes();
        if (StringUtils.isBlank(eventTypes)) {
            return false;
        }
        String expected = eventType;
        for (String item : eventTypes.split(",")) {
            if (expected.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }

    private TgMessageTaskEntity buildTask(TgChatEntity target, String eventType,
                                          String content, String traceId, String parseMode) {
        TgMessageTaskEntity task = new TgMessageTaskEntity();
        task.setTenantId(target.getTenantId());
        task.setMerchantId(target.getMerchantId());
        task.setBotId(target.getBotId());
        task.setChatId(target.getChatId());
        task.setTaskNo(NotifyKeyUtils.messageTaskNo());
        task.setBizType(resolveBizType(eventType));
        task.setBizNo(eventType);
        task.setEventType(eventType);
        task.setSourceEventId(sourceEventId(eventType, target, traceId, content, parseMode));
        task.setParseMode(parseMode);
        task.setContent(StringUtils.defaultString(content));
        task.setPayloadJson(null);
        task.setStatus("INIT");
        task.setRetryCount(0);
        task.setMaxRetryCount(8);
        task.setNextRetryAt(Instant.now());
        task.setTraceId(traceId);
        return task;
    }

    private String sourceEventId(String eventType, TgChatEntity target, String traceId,
                                 String content, String parseMode) {
        String seed = eventType + "|" + target.getBotId() + "|" + target.getChatId() + "|"
                + StringUtils.defaultString(traceId) + "|"
                + StringUtils.defaultString(parseMode) + "|" + StringUtils.defaultString(content);
        return SecureUtil.sha256(seed).substring(0, 32);
    }
}
