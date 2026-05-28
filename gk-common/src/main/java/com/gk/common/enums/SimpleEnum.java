package com.gk.common.enums;

/**
 * 需要前端展示的枚举
 * @param <T> code类型
 */
public interface SimpleEnum<T> extends CodeEnum<T> {
    String i18nKey();
}
