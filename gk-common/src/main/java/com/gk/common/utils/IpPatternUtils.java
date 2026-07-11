package com.gk.common.utils;

import java.util.Collection;

public final class IpPatternUtils {
    private IpPatternUtils() {
    }

    public static boolean matchesAny(String clientIp, Collection<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (matches(clientIp, pattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matches(String clientIp, String pattern) {
        if (clientIp == null || clientIp.isBlank() || pattern == null || pattern.isBlank()) {
            return false;
        }
        String cleanIp = clientIp.trim();
        String cleanPattern = pattern.trim();
        if ("*".equals(cleanPattern) || "0.0.0.0/0".equals(cleanPattern)) {
            return true;
        }
        if (!cleanPattern.contains("/")) {
            return cleanIp.equals(cleanPattern);
        }
        return cidrMatches(cleanIp, cleanPattern);
    }

    private static boolean cidrMatches(String clientIp, String cidr) {
        String[] parts = cidr.split("/");
        if (parts.length != 2) {
            return false;
        }
        try {
            long ip = ipv4ToLong(clientIp);
            long network = ipv4ToLong(parts[0].trim());
            int prefix = Integer.parseInt(parts[1].trim());
            if (prefix < 0 || prefix > 32) {
                return false;
            }
            long mask = prefix == 0 ? 0 : 0xffffffffL << (32 - prefix);
            return (ip & mask) == (network & mask);
        } catch (RuntimeException e) {
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
