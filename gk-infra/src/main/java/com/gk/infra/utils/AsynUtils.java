package com.gk.infra.utils;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsynUtils {
    private static final String DEFAULT_TASK_NAME = "AsyncTask";
//    private static SysBotService sysBotService;

    /**
     * 异步执行任务（带异常日志）
     * @param task 要执行的任务
     */
    public static void execute(Runnable task) {
        execute(DEFAULT_TASK_NAME, task);
    }

    /**
     * 异步执行任务，支持命名和异常上报
     * @param taskName 任务名称（用于日志）
     * @param task 任务
     */
    public static void execute(String taskName, Runnable task) {
        ThreadUtil.execute(() -> {
            try {
                task.run();
            } catch (Throwable e) {
                log.error("[{}] 异步任务执行异常：{}", taskName, e.getMessage(), e);
//                if (sysBotService == null) {
//                    sysBotService = SpringContextUtils.getBean(SysBotService.class);
//                }
//                //异常信息
//                String errorCodeSrc = ExceptionUtils.getErrorStackTraceSrc(e);
//                String content = StringFormat.format("""
//                **{}** 系统出现`异常`,异常信息如下:
//                > 异步任务:  `{}`
//                > 异常时间:  <font color="comment">{}</font>
//                > 异常信息如下:
//                {}""", sysBotService.getAppName(), taskName, DateUtil.now(), errorCodeSrc);
//                sysBotService.sendWeChatMarkdown(SysConstant.BotType.SYS_WARN, content);
            }
        });
    }
}
