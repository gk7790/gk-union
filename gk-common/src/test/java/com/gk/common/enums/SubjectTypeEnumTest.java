package com.gk.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectTypeEnumTest {

    @Test
    void matchesCodeCaseInsensitively() {
        assertTrue(SubjectTypeEnum.TENANT.matches("tenant"));
        assertFalse(SubjectTypeEnum.PLATFORM.matches("TENANT"));
    }

    @Test
    void parsesFromCode() {
        assertEquals(SubjectTypeEnum.MERCHANT, SubjectTypeEnum.fromCode("merchant"));
    }
}
