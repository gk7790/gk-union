package com.gk.openapi.util;

import com.alibaba.fastjson2.JSON;
import com.gk.common.utils.IpPatternUtils;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class IpWhitelistUtils {
    private IpWhitelistUtils() {
    }

    public static boolean allowed(String clientIp, String whitelistJson) {
        List<String> rules = parseRules(whitelistJson);
        return IpPatternUtils.matchesAny(clientIp, rules);
    }

    private static List<String> parseRules(String whitelistJson) {
        if (whitelistJson == null || whitelistJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(whitelistJson, String.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
