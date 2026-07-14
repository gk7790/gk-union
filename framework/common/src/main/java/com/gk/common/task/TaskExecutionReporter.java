package com.gk.common.task;

/**
 * Collects item-level results produced during one scheduled task execution.
 */
public interface TaskExecutionReporter {

    void success(String itemType, String itemId, String message);

    void failure(String itemType, String itemId, String message);
}
