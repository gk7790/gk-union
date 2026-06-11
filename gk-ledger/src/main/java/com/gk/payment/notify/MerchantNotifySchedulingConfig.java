package com.gk.payment.notify;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启 Spring 定时调度(供商户通知发送器使用)。
 * <p>
 * 与 gk-scheduler 的 Quartz 框架互不影响: Quartz 用于业务可配置的定时任务,
 * 这里的 @Scheduled 用于平台内部固定频率的轻量后台循环。
 */
@Configuration
@EnableScheduling
public class MerchantNotifySchedulingConfig {
}
