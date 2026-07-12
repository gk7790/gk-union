package com.gk.merchant.support;

import org.apache.commons.lang3.StringUtils;

/**
 * 商户应用凭证展示规则
 */
public final class MerchantAppSecrets {

    private static final String MASK = "******";

    private MerchantAppSecrets() {
    }

    public static String mask(String apiSecret) {
        String secret = StringUtils.trimToNull(apiSecret);
        if (secret == null) {
            return null;
        }
        if (secret.length() <= 8) {
            return MASK;
        }
        return secret.substring(0, 4) + MASK + secret.substring(secret.length() - 4);
    }
}
