package com.gk.infra.enums;

import com.gk.common.enums.SimpleEnum;

public enum ScopeEnum implements SimpleEnum<Integer> {
    PLATFORM(1, "平台", "enum.platform"),
    TENANT(2, "租户", "enum.tenant"),
    ORG(3, "组织", "enum.org"),
    CLIENT(4, "客户", "enum.client");

    private final Integer code;
    private final String label;
    private final String i18nKey;

    ScopeEnum(int code, String label, String i18nKey) {
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
}
