package com.gk.infra.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.SimpleEnum;
import com.gk.common.enums.StyleType;

import java.util.List;

@EnumDict("status")
public enum StatusEnum implements SimpleEnum<Integer> {
    @Style(StyleType.SUCCESS)
    NORMAL(1, "正常", "enum.status.normal"),

    @Style(StyleType.WARNING)
    PAUSE(2, "暂停", "enum.status.pause"),

    @Style(StyleType.DANGER)
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
