package com.gk.common.utils;

import cn.hutool.core.util.NumberUtil;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberUtils extends NumberUtil {

    // ========== 安全转换 ==========
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 累计加一
     * @param value 原始值
     * @return
     */
    public static BigInteger add36One(String value) {
        BigInteger val = new BigInteger(value, 36); // 从 base36 转十进制
        return val.add(BigInteger.ONE);
    }
}
