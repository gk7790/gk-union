package com.gk.common.enums;

public enum DeletedEnum implements CodeEnum<Integer> {
    NORMAL(0, "正常"),
    DISABLE(1, "删除");

    private final Integer code;
    private final String label;

    DeletedEnum(int code, String label) {
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
