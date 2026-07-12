package com.gk.common.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * 字符串 code 业务枚举通用能力。
 */
public interface StringCodeEnum extends SimpleEnum<String> {

    default boolean matches(String value) {
        return value != null && code().equalsIgnoreCase(value.trim());
    }

    static <E extends Enum<E> & StringCodeEnum> E fromCode(Class<E> type, String value) {
        if (StringUtils.isBlank(value) || type == null) {
            return null;
        }
        for (E item : type.getEnumConstants()) {
            if (item.matches(value)) {
                return item;
            }
        }
        return null;
    }
}
