package com.gk.infra.enums;

import com.gk.common.enums.CodeEnum;

import java.util.List;

public enum StatusEnum implements CodeEnum<Integer> {
    NORMAL(1, "正常"),
    PAUSE(2, "暂停"),
    STOP(3, "停用");

    private final Integer code;
    private final String label;

    StatusEnum(int code, String label) {
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

    public static List<Integer> defaultStatus() {
        return List.of(NORMAL.code, PAUSE.code);
    }
}
