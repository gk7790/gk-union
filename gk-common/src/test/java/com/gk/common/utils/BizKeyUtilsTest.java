package com.gk.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BizKeyUtilsTest {

    @Test
    void generatesAppId() {
        String value = BizKeyUtils.genAppId();

        assertEquals(27, value.length());
        assertTrue(value.matches("G[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{26}"));
    }

    @Test
    void generatesMerchantNo() {
        String value = BizKeyUtils.genMerchantNo();

        assertEquals(16, value.length());
        assertTrue(value.matches("M[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{15}"));
    }

    @Test
    void generatesPspNo() {
        String value = BizKeyUtils.genPspNo();

        assertEquals(16, value.length());
        assertTrue(value.matches("P[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{15}"));
    }

    @Test
    void generatesPayOrderNo() {
        String value = BizKeyUtils.genPayOrderNo();

        assertTrue(value.matches("PAY[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{10,13}"));
    }

    @Test
    void generatesPayoutOrderNo() {
        String value = BizKeyUtils.genPayoutOrderNo();

        assertTrue(value.matches("PAYOUT[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{10,13}"));
    }

    @Test
    void generatesPspRequestNo() {
        String value = BizKeyUtils.genPspRequestNo();

        assertTrue(value.matches("PRQ[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{10,13}"));
    }

    @Test
    void generatesMerchantRequestNo() {
        String value = BizKeyUtils.genMerchantRequestNo();

        assertTrue(value.matches("MRQ[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{10,13}"));
    }

    @Test
    void generatesMerchantNotifyTaskNo() {
        String value = BizKeyUtils.genMerchantNotifyTaskNo();

        assertTrue(value.matches("MNT[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{10,13}"));
    }

    @Test
    void generatesLedgerJournalAndHoldNo() {
        assertTrue(BizKeyUtils.genLedgerJournalNo().matches("LJ[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{10,13}"));
        assertTrue(BizKeyUtils.genLedgerHoldNo().matches("LH[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{10,13}"));
    }

    @Test
    void generatesShortCodeAndNonce() {
        assertTrue(BizKeyUtils.genShortCode().matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}"));
        assertTrue(BizKeyUtils.genNonce().matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{16}"));
    }

    @Test
    void generatesApiSecret() {
        for (int i = 0; i < 100; i++) {
            String value = BizKeyUtils.genApiSecret();

            assertEquals(43, value.length());
            assertTrue(value.matches("[A-Za-z0-9_-]{43}"));
            assertTrue(value.matches("[A-Za-z0-9].*"));
        }
    }

    @Test
    void encodesAndDecodesSnowflakeId() {
        long id = 1990654600050675703L;
        String code = BizKeyUtils.encodeId(id);
        assertEquals(id, BizKeyUtils.decodeId(code));
        assertEquals(id, BizKeyUtils.decodeId(code.toLowerCase()));
        assertTrue(code.matches("[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{10,13}"));
    }

    @Test
    void payOrderNoSuffixMatchesEncodeId() {
        String payOrderNo = BizKeyUtils.genPayOrderNo();
        String suffix = payOrderNo.substring(3);
        assertDoesNotThrow(() -> BizKeyUtils.decodeId(suffix));
    }
}
