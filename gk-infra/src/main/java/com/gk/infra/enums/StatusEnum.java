package com.gk.infra.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.SimpleEnum;

import java.util.List;

@EnumDict("status")
public enum StatusEnum implements SimpleEnum<Integer> {
    NORMAL(1, "正常", "enum.status.normal"),
    PAUSE(2, "暂停", "enum.status.pause"),
    STOP(3, "停用", "enum.status.stop");

    private final Integer code;
    private final String label;
    private final String i18nKey;

    StatusEnum(int code, String label, String i18nKey) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
    }

    @Override
    public Integer code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String i18nKey() {
        return i18nKey;
    }

    public static List<Integer> defaultStatus() {
        return List.of(NORMAL.code, PAUSE.code);
    }
}
