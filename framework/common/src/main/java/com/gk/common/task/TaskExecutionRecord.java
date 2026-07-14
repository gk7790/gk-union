package com.gk.common.task;

import java.util.ArrayList;
import java.util.List;

/**
 * A logical record handled during a task execution.
 */
public final class TaskExecutionRecord {
    private static final TaskExecutionRecord EMPTY = new TaskExecutionRecord("", false);

    private final String name;
    private final boolean enabled;
    private final List<TaskExecutionStep> steps = new ArrayList<>();

    private TaskExecutionRecord(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    static TaskExecutionRecord create(String name) {
        return new TaskExecutionRecord(name, true);
    }

    static TaskExecutionRecord empty() {
        return EMPTY;
    }

    public TaskExecutionRecord step(String name, String message) {
        add(name, message, false);
        return this;
    }

    public TaskExecutionRecord error(String name, String message, Throwable exception) {
        String errorMessage = exception == null
                ? message
                : message + ", error=" + exception.getClass().getSimpleName() + ": " + exception.getMessage();
        add(name, errorMessage, true);
        return this;
    }

    public TaskExecutionRecord complete(String message) {
        add("COMPLETE", message, false);
        return this;
    }

    String name() {
        return name;
    }

    boolean hasError() {
        return steps.stream().anyMatch(TaskExecutionStep::error);
    }

    int stepCount() {
        return steps.size();
    }

    List<TaskExecutionStep> steps() {
        return List.copyOf(steps);
    }

    private void add(String stepName, String message, boolean error) {
        if (!enabled) {
            return;
        }
        steps.add(new TaskExecutionStep(clean(stepName), clean(message), error));
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
