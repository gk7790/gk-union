package com.gk.common.enums;

import java.util.Objects;

public enum AuthTypeEnum implements SimpleEnum<Integer> {
    NONE(0, "None", "enum.authType.none"),
    TOTP(1, "TOTP", "enum.authType.totp");

    private final Integer code;
    private final String label;
    private final String i18nKey;

    AuthTypeEnum(Integer code, String label, String i18nKey) {
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

    public boolean matches(Integer value) {
        return Objects.equals(code, value);
    }

    public static boolean requiresMfa(Integer value) {
        return value != null && !NONE.matches(value);
    }
}
