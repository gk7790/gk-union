package com.gk.payment.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PayinConfigPrecheckResult {
    private boolean passed;
    private List<Item> errors = new ArrayList<>();
    private List<Item> warnings = new ArrayList<>();

    public void addError(String code, String message) {
        errors.add(new Item(code, message));
        passed = false;
    }

    public void addWarning(String code, String message) {
        warnings.add(new Item(code, message));
    }

    public void refreshPassed() {
        passed = errors.isEmpty();
    }

    @Data
    public static class Item {
        private final String code;
        private final String message;
    }
}
