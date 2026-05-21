package com.gk.common.utils;

public class CharUtils {
    private static final char[] CHARSET62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private static final int RADIX = 62;

    public static String numberToBase62(long number) {
        StringBuilder result = new StringBuilder();
        // 当 number 大于 0 时进行转换
        while (number > 0) {
            // 取余数（当前位字符的索引）
            int remainder = (int) (number % 62);
            // 根据余数获取对应字符
            result.append(CHARSET62[remainder]);
            // 更新 number 为商
            number /= 62;
        }
        // 反转结果，因为我们是从低位到高位转换的
        return result.reverse().toString();
    }

    /**
     * Base62 -> long
     */
    public static long decode(String str) {
        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            int index = indexOf(str.charAt(i));
            if (index < 0) {
                throw new IllegalArgumentException("Invalid Base62 char: " + str.charAt(i));
            }
            result = result * RADIX + index;
        }
        return result;
    }

    private static int indexOf(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'Z') return c - 'A' + 10;
        if (c >= 'a' && c <= 'z') return c - 'a' + 36;
        return -1;
    }
}
