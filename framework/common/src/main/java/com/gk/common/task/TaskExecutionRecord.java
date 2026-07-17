package com.gk.common.task;

import java.util.ArrayList;
import java.util.List;

/**
 * A logical record handled during a task execution.
 */
public final class TaskExecutionRecord {
    private static final TaskExecutionRecord EMPTY = new TaskExecutionRecord("", false, null);

    private final String name;
    private final boolean enabled;
    private final Runnable firstErrorCallback;
    private final List<TaskExecutionStep> steps = new ArrayList<>();
    private boolean error;

    private TaskExecutionRecord(String name, boolean enabled, Runnable firstErrorCallback) {
        this.name = name;
        this.enabled = enabled;
        this.firstErrorCallback = firstErrorCallback;
    }

    static TaskExecutionRecord create(String name) {
        return new TaskExecutionRecord(name, true, null);
    }

    static TaskExecutionRecord empty() {
        return EMPTY;
    }

    static TaskExecutionRecord omitted(Runnable firstErrorCallback) {
        return new TaskExecutionRecord("", false, firstErrorCallback);
    }

    public TaskExecutionRecord step(String name, String message) {
        add(name, message, false);
        return this;
    }

    public TaskExecutionRecord error(String name, String message, Throwable exception) {
        markError();
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
        return error;
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

    private void markError() {
        if (error || (!enabled && firstErrorCallback == null)) {
            return;
        }
        error = true;
        if (firstErrorCallback != null) {
            firstErrorCallback.run();
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
