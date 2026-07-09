package com.gk.infra.telegram;

/**
 * Telegram 告警与通知任务创建服务。
 * <p>
 * 业务模块优先调用 {@link #notify(TgAlertEventType, Long, Long, String, String, String)}，
 * 以后新增通知类型只需要扩展 {@link TgAlertEventType}，不需要继续给接口加方法。
 */
public interface TgAlertService {

    /**
     * 异步创建通知任务。
     *
     * @param eventType  通知事件大类
     * @param tenantId   租户 ID，平台级通知可传 0 或 null
     * @param merchantId 商户 ID，非商户级通知可传 0 或 null
     * @param title      通知标题
     * @param content    通知正文
     * @param traceId    链路追踪 ID，可为空
     */
    void notify(TgAlertEventType eventType, Long tenantId, Long merchantId, String title, String content, String traceId);

    /**
     * 异步创建通知任务，并指定 Telegram parse_mode。
     * <p>
     * parseMode 支持 HTML、MarkdownV2、NONE；为空或非法时由实现层回退到 HTML。
     */
    default void notify(TgAlertEventType eventType, Long tenantId, Long merchantId,
                        String title, String content, String traceId, String parseMode) {
        notify(eventType, tenantId, merchantId, title, content, traceId);
    }

    /**
     * 同步创建通知任务。
     * <p>
     * 适合服务停止等关闭阶段使用，避免异步线程尚未执行应用就退出。
     */
    default void notifySync(TgAlertEventType eventType, Long tenantId, Long merchantId,
                            String title, String content, String traceId) {
        notify(eventType, tenantId, merchantId, title, content, traceId);
    }

    /**
     * 同步创建通知任务，并指定 Telegram parse_mode。
     */
    default void notifySync(TgAlertEventType eventType, Long tenantId, Long merchantId,
                            String title, String content, String traceId, String parseMode) {
        notifySync(eventType, tenantId, merchantId, title, content, traceId);
    }

    /** 创建系统错误通知任务。 */
    default void sysError(String title, String content, String traceId) {
        sysError(0L, 0L, title, content, traceId);
    }

    /** 创建带租户/商户上下文的系统错误通知任务。 */
    default void sysError(Long tenantId, Long merchantId, String title, String content, String traceId) {
        notify(TgAlertEventType.SYSTEM_ERROR, tenantId, merchantId, title, content, traceId);
    }

    /** 创建系统警告通知任务。 */
    default void sysWarn(String title, String content, String traceId) {
        sysWarn(0L, 0L, title, content, traceId);
    }

    /** 创建带租户/商户上下文的系统警告通知任务。 */
    default void sysWarn(Long tenantId, Long merchantId, String title, String content, String traceId) {
        notify(TgAlertEventType.SYSTEM_WARN, tenantId, merchantId, title, content, traceId);
    }

    /** 同步创建系统警告通知任务。 */
    default void sysWarnSync(String title, String content, String traceId) {
        sysWarnSync(0L, 0L, title, content, traceId);
    }

    /** 同步创建带租户/商户上下文的系统警告通知任务。 */
    default void sysWarnSync(Long tenantId, Long merchantId, String title, String content, String traceId) {
        notifySync(TgAlertEventType.SYSTEM_WARN, tenantId, merchantId, title, content, traceId);
    }

    /** 创建风控提醒通知任务。 */
    default void riskAlert(Long tenantId, Long merchantId, String title, String content, String traceId) {
        notify(TgAlertEventType.RISK_WARN, tenantId, merchantId, title, content, traceId);
    }

    /** 创建代收相关通知任务。 */
    default void paySuccess(Long tenantId, Long merchantId, String title, String content, String traceId) {
        notify(TgAlertEventType.PAYIN_NOTICE, tenantId, merchantId, title, content, traceId);
    }

    /** 创建代付成功相关通知任务。 */
    default void payoutSuccess(Long tenantId, Long merchantId, String title, String content, String traceId) {
        notify(TgAlertEventType.PAYOUT_NOTICE, tenantId, merchantId, title, content, traceId);
    }

    /** 创建代付失败相关通知任务。 */
    default void payoutFailed(Long tenantId, Long merchantId, String title, String content, String traceId) {
        notify(TgAlertEventType.PAYOUT_NOTICE, tenantId, merchantId, title, content, traceId);
    }
}
