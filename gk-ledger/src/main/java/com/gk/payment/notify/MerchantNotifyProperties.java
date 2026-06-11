package com.gk.payment.notify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商户异步通知发送器配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "merchant.notify")
public class MerchantNotifyProperties {
    /** 是否启用通知发送器(定时扫描) */
    private boolean enabled = true;
    /** 扫描间隔(毫秒) */
    private long scanIntervalMs = 5000;
    /** 启动后首次扫描延迟(毫秒), 给应用预热 */
    private long initialDelayMs = 10000;
    /** 单次扫描抢占的任务数 */
    private int batchSize = 50;
    /** 任务锁定时长(秒): 抢占后多久未完成视为可被其他节点重新抢占 */
    private int lockSeconds = 60;
    /** HTTP 连接超时(毫秒) */
    private int connectTimeoutMs = 3000;
    /** 任务未配置 timeoutMs 时的默认读取超时(毫秒) */
    private int defaultTimeoutMs = 5000;
    /** 是否要求响应体包含成功标识才算成功(类似支付宝/微信要求返回 success) */
    private boolean requireSuccessBody = true;
    /** 视为成功的响应体标识(忽略大小写, 命中其一即成功) */
    private List<String> successBodyTokens = List.of("success", "ok");
    /** 重试退避秒数(按已失败次数取下标, 超出取最后一个) */
    private List<Long> retryBackoffSeconds = List.of(15L, 30L, 60L, 120L, 300L, 600L, 1800L, 3600L, 7200L, 21600L);
    /** 落库的响应体/错误信息最大长度 */
    private int maxStoreBodyLength = 4000;
    /** 当前节点标识(为空则自动生成) */
    private String workerId;

    /**
     * 根据"已失败次数"计算下次重试的退避秒数。
     */
    public long backoffSeconds(int failedTimes) {
        if (retryBackoffSeconds == null || retryBackoffSeconds.isEmpty()) {
            return 60L;
        }
        int index = Math.max(0, failedTimes - 1);
        if (index >= retryBackoffSeconds.size()) {
            index = retryBackoffSeconds.size() - 1;
        }
        return retryBackoffSeconds.get(index);
    }
}
