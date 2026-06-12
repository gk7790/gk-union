package com.gk.common.enums;

/**
 * 登录/数据权限主体类型。
 */
public enum SubjectTypeEnum implements StringCodeEnum {
    PLATFORM("PLATFORM", "平台", "enum.subjectType.platform"),
    TENANT("TENANT", "租户", "enum.subjectType.tenant"),
    MERCHANT("MERCHANT", "商户", "enum.subjectType.merchant");

    private final String code;
    private final String label;
    private final String i18nKey;

    SubjectTypeEnum(String code, String label, String i18nKey) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
    }

    @Override
    public String code() {
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

    public static SubjectTypeEnum fromCode(String value) {
        return StringCodeEnum.fromCode(SubjectTypeEnum.class, value);
    }
}
