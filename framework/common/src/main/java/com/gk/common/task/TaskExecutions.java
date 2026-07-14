package com.gk.common.task;

/**
 * Keeps the current synchronous task execution available to task code.
 */
public final class TaskExecutions {
    private static final ThreadLocal<TaskExecution> CURRENT = new ThreadLocal<>();

    private TaskExecutions() {
    }

    public static TaskExecution start(String name) {
        TaskExecution execution = TaskExecution.create(name);
        CURRENT.set(execution);
        return execution;
    }

    public static TaskExecution current() {
        TaskExecution execution = CURRENT.get();
        return execution == null ? TaskExecution.empty() : execution;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
