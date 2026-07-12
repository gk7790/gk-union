package com.gk.common.enums;

public enum EnabledEnum implements CodeEnum<Integer> {
    ENABLE(1, "启用"),
    DISABLE(0, "禁用");

    private final Integer code;
    private final String label;

    EnabledEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public Integer code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }
}
