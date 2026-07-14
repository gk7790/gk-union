package com.gk.common.task;

/**
 * A single operation recorded during a task execution.
 */
public record TaskExecutionStep(String name, String message, boolean error) {
}
