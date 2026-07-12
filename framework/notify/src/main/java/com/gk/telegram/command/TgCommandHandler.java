package com.gk.telegram.command;

/**
 * 入站指令处理器; 每个指令实现一个Bean, 由 {@link TgCommandDispatcher} 自动收集分发
 */
public interface TgCommandHandler {
    /**
     * 指令(小写, 含前导/, 如 /balance)
     */
    String command();

    /**
     * 指令说明(用于 /help 列表)
     */
    String description();

    /**
     * 是否要求已绑定系统账号; true时未绑定将提示先 /bind
     */
    default boolean requireBinding() {
        return true;
    }

    /**
     * 处理指令, 返回回复文本
     */
    String handle(TgCommandContext ctx);
}
