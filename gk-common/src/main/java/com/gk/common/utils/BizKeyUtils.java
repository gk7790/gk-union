package com.gk.common.utils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.security.SecureRandom;
import java.util.Base64;

public class BizKeyUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] UPPER_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final char[] BASE32_CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final int APP_ID_RANDOM_LENGTH = 26;
    private static final int MERCHANT_NO_RANDOM_LENGTH = 15;
    private static final int PSP_NO_RANDOM_LENGTH = 15;
    private static final int SHORT_CODE_LENGTH = 8;
    private static final int NONCE_LENGTH = 16;
    private static final int API_SECRET_BYTES = 32;

    private BizKeyUtils() {
    }

    public static String genAppId() {
        return "G" + randomUpperCode(APP_ID_RANDOM_LENGTH);
    }

    public static String genMerchantNo() {
        return "M" + randomUpperCode(MERCHANT_NO_RANDOM_LENGTH);
    }

    public static String genPspNo() {
        return "P" + randomUpperCode(PSP_NO_RANDOM_LENGTH);
    }

    public static String genPayOrderNo() {
        return "PAY" + toBase32(IdWorker.getId());
    }

    public static String genPspRequestNo() {
        return "PRQ" + toBase32(IdWorker.getId());
    }

    public static String genShortCode() {
        return randomUpperCode(SHORT_CODE_LENGTH);
    }

    public static String genNonce() {
        return randomUpperCode(NONCE_LENGTH);
    }

    public static String genApiSecret() {
        String secret;
        do {
            byte[] bytes = new byte[API_SECRET_BYTES];
            RANDOM.nextBytes(bytes);
            secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (secret.startsWith("-") || secret.startsWith("_"));
        return secret;
    }

    private static String randomUpperCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(UPPER_CHARS[RANDOM.nextInt(UPPER_CHARS.length)]);
        }
        return code.toString();
    }

    private static String toBase32(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
        StringBuilder result = new StringBuilder(13);
        long current = value;
        while (current > 0) {
            result.append(BASE32_CHARS[(int) (current & 31)]);
            current >>>= 5;
        }
        return result.reverse().toString();
    }
}
