package com.gk.infra.telegram;

import com.alibaba.fastjson2.JSON;
import com.gk.common.constant.Constant;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.infra.config.model.TgBaseConfig;
import com.gk.infra.config.service.SysParamsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Telegram 绑定票据服务。
 */
@Component
@RequiredArgsConstructor
public class TgBindTicketService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final int MAX_GENERATE_ATTEMPTS = 5;

    private final RedisUtils redisUtils;
    private final SysParamsService sysParamsService;
    private final SecureRandom random = new SecureRandom();

    public TgBindTicket generate(TgBindTicketCreateRequest request) {
        validateCreateRequest(request);
        long ttlSeconds = getTtlSeconds();
        for (int i = 0; i < MAX_GENERATE_ATTEMPTS; i++) {
            String code = randomCode();
            String key = RedisKeys.getTgBindTicketKey(code);
            if (!redisUtils.isKeyExist(key)) {
                TgBindTicket ticket = new TgBindTicket();
                ticket.setCode(code);
                ticket.setPurpose(request.getPurpose().name());
                ticket.setTenantId(request.getTenantId());
                ticket.setMerchantId(request.getMerchantId());
                ticket.setSubjectId(request.getSubjectId());
                ticket.setUserId(request.getUserId());
                redisUtils.set(key, JSON.toJSONString(ticket), ttlSeconds);
                return ticket;
            }
        }
        throw new IllegalStateException("Failed to generate Telegram bind ticket");
    }

    public TgBindTicket consume(String code, TgBindPurpose expectedPurpose) {
        String normalized = normalize(code);
        if (normalized == null || expectedPurpose == null) {
            return null;
        }
        String key = RedisKeys.getTgBindTicketKey(normalized);
        TgBindTicket ticket = parseTicket(redisUtils.get(key));
        if (!matches(ticket, expectedPurpose)) {
            return null;
        }
        TgBindTicket consumed = parseTicket(redisUtils.getAndDelete(key));
        if (!matches(consumed, expectedPurpose)) {
            return null;
        }
        return consumed;
    }

    private void validateCreateRequest(TgBindTicketCreateRequest request) {
        if (request == null || request.getPurpose() == null) {
            throw new IllegalArgumentException("purpose is required");
        }
        if (request.getTenantId() == null || request.getMerchantId() == null
                || request.getSubjectId() == null || request.getUserId() == null) {
            throw new IllegalArgumentException("tenantId, merchantId, subjectId and userId are required");
        }
    }

    private boolean matches(TgBindTicket ticket, TgBindPurpose expectedPurpose) {
        return ticket != null && ticket.purposeMatches(expectedPurpose);
    }

    private TgBindTicket parseTicket(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.parseObject(String.valueOf(value), TgBindTicket.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private long getTtlSeconds() {
        TgBaseConfig tgBase = sysParamsService.getValueObject(Constant.TELEGRAM_BASE_CONFIG_KEY, TgBaseConfig.class);
        int ttlMinutes = tgBase != null && tgBase.getBindCodeTtl() != null ? tgBase.getBindCodeTtl() : 10;
        return Math.max(1, ttlMinutes) * 60L;
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
}
