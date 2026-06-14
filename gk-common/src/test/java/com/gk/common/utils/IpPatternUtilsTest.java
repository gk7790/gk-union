package com.gk.common.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpPatternUtilsTest {

    @Test
    void matchesExactIpv4() {
        assertTrue(IpPatternUtils.matches("10.0.0.8", "10.0.0.8"));
        assertFalse(IpPatternUtils.matches("10.0.0.9", "10.0.0.8"));
    }

    @Test
    void matchesCidrIpv4() {
        assertTrue(IpPatternUtils.matches("192.168.1.10", "192.168.1.0/24"));
        assertFalse(IpPatternUtils.matches("192.168.2.10", "192.168.1.0/24"));
    }

    @Test
    void matchesWildcardRules() {
        assertTrue(IpPatternUtils.matches("8.8.8.8", "*"));
        assertTrue(IpPatternUtils.matches("8.8.8.8", "0.0.0.0/0"));
    }

    @Test
    void emptyOrInvalidRulesReject() {
        assertFalse(IpPatternUtils.matchesAny("10.0.0.8", List.of()));
        assertFalse(IpPatternUtils.matches("10.0.0.8", "10.0.0.0/99"));
        assertFalse(IpPatternUtils.matches("not-an-ip", "10.0.0.0/24"));
    }
}
