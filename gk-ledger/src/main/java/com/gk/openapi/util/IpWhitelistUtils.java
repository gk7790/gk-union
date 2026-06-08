package com.gk.openapi.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public class IpWhitelistUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private IpWhitelistUtils() {
    }

    public static boolean allowed(String clientIp, String whitelistJson) {
        List<String> rules = parseRules(whitelistJson);
        if (rules.isEmpty()) {
            return false;
        }
        for (String rule : rules) {
            if (matches(clientIp, rule)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> parseRules(String whitelistJson) {
        if (whitelistJson == null || whitelistJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(whitelistJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static boolean matches(String clientIp, String rule) {
        if (clientIp == null || clientIp.isBlank() || rule == null || rule.isBlank()) {
            return false;
        }
        String cleanRule = rule.trim();
        if ("*".equals(cleanRule) || "0.0.0.0/0".equals(cleanRule)) {
            return true;
        }
        if (!cleanRule.contains("/")) {
            return clientIp.equals(cleanRule);
        }
        return cidrMatches(clientIp, cleanRule);
    }

    private static boolean cidrMatches(String clientIp, String cidr) {
        String[] parts = cidr.split("/");
        if (parts.length != 2) {
            return false;
        }
        try {
            long ip = ipv4ToLong(clientIp);
            long network = ipv4ToLong(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > 32) {
                return false;
            }
            long mask = prefix == 0 ? 0 : 0xffffffffL << (32 - prefix);
            return (ip & mask) == (network & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private static long ipv4ToLong(String ip) {
        String[] segments = ip.split("\\.");
        if (segments.length != 4) {
            throw new IllegalArgumentException("Only IPv4 is supported");
        }
        long result = 0;
        for (String segment : segments) {
            int value = Integer.parseInt(segment);
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException("Invalid IPv4 segment");
            }
            result = (result << 8) + value;
        }
        return result;
    }
}
