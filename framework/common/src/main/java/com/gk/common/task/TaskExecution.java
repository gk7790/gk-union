package com.gk.common.task;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the records and steps for one task invocation.
 */
public final class TaskExecution {
    private static final int MAX_RECORDS = 200;
    private static final TaskExecution EMPTY = new TaskExecution("", false);

    private final String name;
    private final boolean enabled;
    private final List<TaskExecutionRecord> records = new ArrayList<>();
    private int omittedRecords;

    private TaskExecution(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    static TaskExecution create(String name) {
        return new TaskExecution(name, true);
    }

    static TaskExecution empty() {
        return EMPTY;
    }

    public TaskExecutionRecord record(String name) {
        if (!enabled) {
            return TaskExecutionRecord.empty();
        }
        if (records.size() >= MAX_RECORDS) {
            omittedRecords++;
            return TaskExecutionRecord.empty();
        }
        TaskExecutionRecord record = TaskExecutionRecord.create(name);
        records.add(record);
        return record;
    }

    public String summary(String taskResult) {
        if (!enabled || records.isEmpty()) {
            return taskResult;
        }
        StringBuilder builder = new StringBuilder("Execution: ").append(name).append('\n');
        int errors = 0;
        int steps = 0;
        for (TaskExecutionRecord record : records) {
            builder.append("Record: ").append(record.name()).append('\n');
            for (TaskExecutionStep step : record.steps()) {
                builder.append("  - ").append(step.error() ? "ERROR " : "STEP ")
                        .append(step.name()).append(": ").append(step.message()).append('\n');
            }
            if (record.hasError()) {
                errors++;
            }
            steps += record.stepCount();
        }
        if (omittedRecords > 0) {
            builder.append("Records omitted: ").append(omittedRecords).append('\n');
        }
        builder.append("Summary: records=").append(records.size() + omittedRecords)
                .append(", recordsWithErrors=").append(errors)
                .append(", steps=").append(steps);
        if (taskResult != null && !taskResult.isBlank()) {
            builder.append(", result=").append(taskResult);
        }
        return builder.toString();
    }
}
