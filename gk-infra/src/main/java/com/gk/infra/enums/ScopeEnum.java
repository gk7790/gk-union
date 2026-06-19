package com.gk.infra.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.SimpleEnum;
import com.gk.common.enums.StyleType;

import java.util.ArrayList;
import java.util.List;

@EnumDict("scope")
public enum ScopeEnum implements SimpleEnum<Integer> {
    @Style(StyleType.PRIMARY)
    PLATFORM(1, "平台", "dict.scope.platform"),

    @Style(StyleType.SUCCESS)
    TENANT(3, "租户", "dict.scope.tenant"),

    @Style(StyleType.INFO)
    ORG(5, "组织", "dict.scope.org"),

    @Style(StyleType.WARNING)
    AGENT(7, "代理", "dict.scope.agent");

    private final Integer code;
    private final String label;
    private final String i18nKey;

    ScopeEnum(int code, String label, String i18nKey) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
    }

    public static List<Integer> sysList() {
        return List.of(PLATFORM.code, TENANT.code, ORG.code, AGENT.code);
    }

    public static List<ScopeEnum> scope(int scope) {
        List<ScopeEnum> result = new ArrayList<>();
        for (ScopeEnum value : ScopeEnum.values()) {
            if (value.code > scope) {
                result.add(value);
            }
        }
        return result;
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
