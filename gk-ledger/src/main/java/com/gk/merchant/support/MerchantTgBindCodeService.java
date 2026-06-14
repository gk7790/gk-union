package com.gk.merchant.support;

import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.infra.config.model.TgBaseConfig;
import com.gk.infra.config.service.SysParamsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class MerchantTgBindCodeService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final int MAX_GENERATE_ATTEMPTS = 5;

    private final RedisUtils redisUtils;
    private final SysParamsService sysParamsService;
    private final SecureRandom random = new SecureRandom();

    public String generate(Long merchantId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("merchantId must not be null");
        }
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++) {
            String code = randomCode();
            String key = RedisKeys.getTgMerchantBindCodeKey(code);
            if (!redisUtils.isKeyExist(key)) {
                TgBaseConfig tgBase = sysParamsService.getValueObject(Constant.TELEGRAM_BASE_CONFIG_KEY, TgBaseConfig.class);
                redisUtils.set(key, String.valueOf(merchantId), tgBase.getBindCodeTtl() * 60L);
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate merchant Telegram bind code");
    }

    public Long consume(String code) {
        String normalized = normalize(code);
        if (normalized == null) {
            return null;
        }
        String key = RedisKeys.getTgMerchantBindCodeKey(normalized);
        Object value = redisUtils.getAndDelete(key);
        if (value == null) {
            return null;
        }
        Long merchantId = parseMerchantId(value);
        if (merchantId == null) {
            return null;
        }
        return merchantId;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private String normalize(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return code.trim().toUpperCase();
    }

    private Long parseMerchantId(Object value) {
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
