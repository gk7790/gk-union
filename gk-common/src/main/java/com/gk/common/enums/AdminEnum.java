package com.gk.common.enums;

public enum AdminEnum implements CodeEnum<Integer> {

    YES(1, "是"),
    NO(0, "否");

    private final Integer code;
    private final String label;

    AdminEnum(Integer code, String label) {
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