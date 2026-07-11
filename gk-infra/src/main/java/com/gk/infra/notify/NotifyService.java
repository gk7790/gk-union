package com.gk.infra.notify;

/**
 * Channel-neutral notification service contract.
 * Event type is a stable dictionary code from tgAlertEventType, not a Java enum.
 */
public interface NotifyService {
    void notify(String eventType, Long tenantId, Long merchantId, String content, String traceId);

    default void notify(String eventType, Long tenantId, Long merchantId,
                        String content, String traceId, String parseMode) {
        notify(eventType, tenantId, merchantId, content, traceId);
    }

    default void notifySync(String eventType, Long tenantId, Long merchantId,
                            String content, String traceId) {
        notify(eventType, tenantId, merchantId, content, traceId);
    }

    default void notifySync(String eventType, Long tenantId, Long merchantId,
                            String content, String traceId, String parseMode) {
        notifySync(eventType, tenantId, merchantId, content, traceId);
    }

    default void sysError(Long tenantId, Long merchantId, String content, String traceId) {
        notify(NotifyEventCodes.SYSTEM_ERROR, tenantId, merchantId, content, traceId);
    }

    default void sysWarn(String content, String traceId) {
        sysWarn(0L, 0L, content, traceId);
    }

    default void sysWarn(Long tenantId, Long merchantId, String content, String traceId) {
        notify(NotifyEventCodes.SYSTEM_WARN, tenantId, merchantId, content, traceId);
    }

    default void sysWarnSync(String content, String traceId) {
        sysWarnSync(0L, 0L, content, traceId);
    }

    default void sysWarnSync(Long tenantId, Long merchantId, String content, String traceId) {
        notifySync(NotifyEventCodes.SYSTEM_WARN, tenantId, merchantId, content, traceId);
    }
}
