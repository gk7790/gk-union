package com.gk.common.utils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.gk.common.enums.PayDirectionEnum;

import java.security.SecureRandom;
import java.util.Base64;

public class BizKeyUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] UPPER_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final char[] BASE32_CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final int APP_ID_RANDOM_LENGTH = 26;
    private static final int SHORT_CODE_LENGTH = 8;
    private static final int API_SECRET_BYTES = 32;

    private BizKeyUtils() {
    }

    public static String genAppId() {
        return "G" + randomUpperCode(APP_ID_RANDOM_LENGTH);
    }

    public static String genMerchantNo() {
        return "M" + encodeId(IdWorker.getId());
    }

    public static String genPayinOrderNo() {
        return PayDirectionEnum.PAYIN.code() + encodeId(IdWorker.getId());
    }

    public static String genPayoutOrderNo() {
        return PayDirectionEnum.PAYOUT.code() + encodeId(IdWorker.getId());
    }

    public static String genPspRequestNo() {
        return "PRQ" + encodeId(IdWorker.getId());
    }

    public static String genMerchantRequestNo() {
        return "MRQ" + encodeId(IdWorker.getId());
    }

    public static String genMerchantNotifyTaskNo() {
        return "MNT" + encodeId(IdWorker.getId());
    }

    public static String genEventId() {
        return "EVT" + encodeId(IdWorker.getId());
    }

    public static String genTgMessageTaskNo() {
        return "TGM" + encodeId(IdWorker.getId());
    }

    public static String genOrderStatusLogNo() {
        return "OSL" + encodeId(IdWorker.getId());
    }

    public static String genLedgerJournalNo() {
        return "LJ" + encodeId(IdWorker.getId());
    }

    public static String genLedgerHoldNo() {
        return "LH" + encodeId(IdWorker.getId());
    }

    public static String genMerchantBalanceAdjustOrderNo() {
        return "MBA" + encodeId(IdWorker.getId());
    }

    public static String genPspAccountNo() {
        return "PA" + randomUpperCode(SHORT_CODE_LENGTH);
    }

    public static String genShortCode() {
        return randomUpperCode(SHORT_CODE_LENGTH);
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

    /**
     * 将雪花 ID 编码成短 Base32 字符串，用作各类业务单号后缀。
     */
    public static String encodeId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        StringBuilder result = new StringBuilder(13);
        long current = id;
        while (current > 0) {
            result.append(BASE32_CHARS[(int) (current & 31)]);
            current >>>= 5;
        }
        return result.reverse().toString();
    }

    private static String randomUpperCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(UPPER_CHARS[RANDOM.nextInt(UPPER_CHARS.length)]);
        }
        return code.toString();
    }
}
