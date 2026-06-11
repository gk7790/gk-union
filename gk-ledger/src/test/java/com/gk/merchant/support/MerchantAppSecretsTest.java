package com.gk.merchant.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MerchantAppSecretsTest {

    @Test
    void masksLongSecret() {
        assertEquals("94K-******Fj8", MerchantAppSecrets.mask("94K-hU41SvABlROfdMDd-VpAi5SyZVDFjKecJzqmFj8"));
    }

    @Test
    void masksShortSecret() {
        assertEquals("******", MerchantAppSecrets.mask("short"));
    }

    @Test
    void returnsNullForBlank() {
        assertNull(MerchantAppSecrets.mask(null));
        assertNull(MerchantAppSecrets.mask(" "));
    }
}
