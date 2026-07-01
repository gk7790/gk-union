package com.gk.infra.telegram;

/**
 * Telegram 系统预警服务接口。
 * <p>接口放在 infra，业务模块可直接依赖；具体发送实现由 notify 模块提供。</p>
 */
public interface TgBotService {
    /**
     * 创建系统错误预警消息任务。
     */
    void sysError(String title, String content, String traceId);
    void sysError(Long tenantId, Long merchantId, String title, String content, String traceId);

    /**
     * 创建系统警告预警消息任务。
     */
    void sysWarn(String title, String content, String traceId);
    void sysWarn(Long tenantId, Long merchantId, String title, String content, String traceId);

    /**
     * 创建风控预警消息任务。
     */
    void riskAlert(Long tenantId, Long merchantId, String title, String content, String traceId);

    /**
     * 创建支付成功通知消息任务。
     */
    void paySuccess(Long tenantId, Long merchantId, String title, String content, String traceId);

    /**
     * 创建代付成功通知消息任务。
     */
    void payoutSuccess(Long tenantId, Long merchantId, String title, String content, String traceId);

    /**
     * 创建代付失败通知消息任务。
     */
    void payoutFailed(Long tenantId, Long merchantId, String title, String content, String traceId);
}
