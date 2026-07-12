package com.gk.common.utils;

import cn.hutool.core.util.NumberUtil;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberUtils extends NumberUtil {

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
     * value != nulll and value > 0
     * @param value
     * @return
     */
    public static boolean isPositive(Number value) {
        if (value == null) {
            return false;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.compareTo(BigDecimal.ZERO) > 0;
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.compareTo(BigInteger.ZERO) > 0;
        }
        if (value instanceof Double doubleValue) {
            return !doubleValue.isNaN() && !doubleValue.isInfinite() && doubleValue > 0;
        }
        if (value instanceof Float floatValue) {
            return !floatValue.isNaN() && !floatValue.isInfinite() && floatValue > 0;
        }
        return value.longValue() > 0;
    }

    public static boolean isGreaterThanZero(Number value) {
        return isPositive(value);
    }

    public static BigInteger add36One(String value) {
        BigInteger val = new BigInteger(value, 36);
        return val.add(BigInteger.ONE);
    }
}
