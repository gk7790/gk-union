package com.gk.telegram.support;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.security.SecureRandom;
import java.util.Base64;

public final class NotifyKeyUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final char[] BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private NotifyKeyUtils() {}

    public static String messageTaskNo() { return "TGM" + encode(IdWorker.getId()); }
    public static String shortCode() { return randomCode(8); }
    public static String secret() {
        byte[] bytes = new byte[32];
        String value;
        do {
            RANDOM.nextBytes(bytes);
            value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (value.startsWith("-") || value.startsWith("_"));
        return value;
    }

    private static String randomCode(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) value.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        return value.toString();
    }

    private static String encode(long id) {
        StringBuilder value = new StringBuilder(13);
        while (id > 0) {
            value.append(BASE32[(int) (id & 31)]);
            id >>>= 5;
        }
        return value.reverse().toString();
    }
}
