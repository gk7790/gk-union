package com.gk.common.utils;

import cn.hutool.core.util.ObjectUtil;

public class ValueUtils {

    @SuppressWarnings("unchecked")
    public static <T> T defaultValue(Object obj, T defaultValue) {
        return ObjectUtil.isNotEmpty(obj) ? (T) obj : defaultValue;
    }
}
