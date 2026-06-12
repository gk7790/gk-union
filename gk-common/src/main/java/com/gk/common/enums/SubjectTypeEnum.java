package com.gk.common.enums;

import com.gk.common.dto.LabelDTO;

import java.util.List;

/**
 * 登录/数据权限主体类型。
 */
public enum SubjectTypeEnum implements StringCodeEnum {
    PLATFORM("PLATFORM", "平台", "enum.subjectType.platform"),
    TENANT("TENANT", "租户", "enum.subjectType.tenant"),
    MERCHANT("MERCHANT", "商户", "enum.subjectType.merchant");

    private static final String INTERNAL_LABEL = "内部";
    private static final String INTERNAL_I18N_KEY = "enum.subjectType.internal";

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

    public static List<LabelDTO> visibleList(String subjectType) {
        return visibleTypes(subjectType).stream()
                .map(item -> item.toVisibleLabel(subjectType))
                .toList();
    }

    private static List<SubjectTypeEnum> visibleTypes(String subjectType) {
        if (subjectType == null || subjectType.isBlank()) {
            return List.of(PLATFORM, TENANT, MERCHANT);
        }
        if (PLATFORM.matches(subjectType)) {
            return List.of(PLATFORM, TENANT, MERCHANT);
        }
        if (TENANT.matches(subjectType)) {
            return List.of(TENANT, MERCHANT);
        }
        if (MERCHANT.matches(subjectType)) {
            return List.of(MERCHANT);
        }
        return List.of(PLATFORM, TENANT, MERCHANT);
    }

    private LabelDTO toVisibleLabel(String currentSubjectType) {
        if (currentSubjectType != null && !currentSubjectType.isBlank() && matches(currentSubjectType)) {
            return new LabelDTO(code(), INTERNAL_LABEL, INTERNAL_I18N_KEY);
        }
        return new LabelDTO(code(), label(), i18nKey());
    }
}
