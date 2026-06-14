package com.gk.infra.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.SimpleEnum;

@EnumDict("domain")
public enum DomainEnum implements SimpleEnum<Integer> {
    GAMING(1, "游戏", "dict.domain.gaming"),
    CLOAK(2, "斗篷", "dict.domain.cloak"),;

    private final int code;
    private final String label;
    private final String i18nKey;

    DomainEnum(int code, String label, String i18nKey) {
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
