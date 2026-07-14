package com.gk.common.task;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the item-level execution report for the current scheduled task thread.
 */
public final class TaskExecutionReports {
    private static final int MAX_RECORDS = 200;
    private static final int MAX_TEXT_LENGTH = 300;
    private static final ThreadLocal<Report> REPORT = new ThreadLocal<>();
    private static final TaskExecutionReporter NO_OP = new TaskExecutionReporter() {
        @Override
        public void success(String itemType, String itemId, String message) {
        }

        @Override
        public void failure(String itemType, String itemId, String message) {
        }
    };

    private TaskExecutionReports() {
    }

    public static void start() {
        REPORT.set(new Report());
    }

    public static TaskExecutionReporter current() {
        Report report = REPORT.get();
        return report == null ? NO_OP : report;
    }

    public static String format(String taskResult) {
        Report report = REPORT.get();
        return report == null || report.total == 0 ? taskResult : report.format(taskResult);
    }

    public static void clear() {
        REPORT.remove();
    }

    private static final class Report implements TaskExecutionReporter {
        private final List<ItemResult> items = new ArrayList<>();
        private int total;
        private int success;
        private int failed;
        private int omitted;

        @Override
        public void success(String itemType, String itemId, String message) {
            record(true, itemType, itemId, message);
        }

        @Override
        public void failure(String itemType, String itemId, String message) {
            record(false, itemType, itemId, message);
        }

        private void record(boolean succeeded, String itemType, String itemId, String message) {
            total++;
            if (succeeded) {
                success++;
            } else {
                failed++;
            }
            if (items.size() >= MAX_RECORDS) {
                omitted++;
                return;
            }
            items.add(new ItemResult(succeeded, clean(itemType), clean(itemId), clean(message)));
        }

        private String format(String taskResult) {
            StringBuilder builder = new StringBuilder();
            for (ItemResult item : items) {
                builder.append(item.succeeded ? "[SUCCESS] " : "[FAIL] ")
                        .append(item.itemType)
                        .append(" id=").append(item.itemId);
                if (!item.message.equals("-")) {
                    builder.append(" - ").append(item.message);
                }
                builder.append('\n');
            }
            if (omitted > 0) {
                builder.append("[OMITTED] ").append(omitted)
                        .append(" item records omitted after the first ").append(MAX_RECORDS).append('\n');
            }
            builder.append("[SUMMARY] total=").append(total)
                    .append(", success=").append(success)
                    .append(", failed=").append(failed);
            if (taskResult != null && !taskResult.isBlank()) {
                builder.append(", taskResult=").append(taskResult);
            }
            return builder.toString();
        }

        private String clean(String value) {
            if (value == null || value.isBlank()) {
                return "-";
            }
            String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
            return normalized.length() <= MAX_TEXT_LENGTH
                    ? normalized
                    : normalized.substring(0, MAX_TEXT_LENGTH - 3) + "...";
        }
    }

    private record ItemResult(boolean succeeded, String itemType, String itemId, String message) {
    }
}
