package com.gk.psp.callback.support;

import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PspCallbackValidatorTest {
    private final PspCallbackValidator validator = new PspCallbackValidator();

    @Test
    void acceptsMatchingTerminalCallback() {
        PspCallbackResult result = result("WORLD", "SUCCESS", "100.00", "PHP");
        PspCallbackOrder order = order("WORLD", "100.00000000", "PHP");

        assertThatCode(() -> validator.validateTerminalCallback(PspCallbackConstants.BIZ_TYPE_PAY_ORDER, result, order))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMismatchedAmount() {
        PspCallbackResult result = result("WORLD", "SUCCESS", "99.99", "PHP");
        PspCallbackOrder order = order("WORLD", "100.00000000", "PHP");

        assertThatThrownBy(() -> validator.validateTerminalCallback(PspCallbackConstants.BIZ_TYPE_PAY_ORDER, result, order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void rejectsMismatchedCurrency() {
        PspCallbackResult result = result("WORLD", "SUCCESS", "100.00", "USD");
        PspCallbackOrder order = order("WORLD", "100.00000000", "PHP");

        assertThatThrownBy(() -> validator.validateTerminalCallback(PspCallbackConstants.BIZ_TYPE_PAY_ORDER, result, order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsMismatchedPspCode() {
        PspCallbackResult result = result("OTHER", "SUCCESS", "100.00", "PHP");
        PspCallbackOrder order = order("WORLD", "100.00000000", "PHP");

        assertThatThrownBy(() -> validator.validateTerminalCallback(PspCallbackConstants.BIZ_TYPE_PAY_ORDER, result, order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PSP");
    }

    private PspCallbackResult result(String pspCode, String orderStatus, String amount, String currency) {
        PspCallbackResult result = new PspCallbackResult();
        result.setPspCode(pspCode);
        result.setOrderStatus(orderStatus);
        result.setAmount(new BigDecimal(amount));
        result.setCurrency(currency);
        return result;
    }

    private PspCallbackOrder order(String pspCode, String amount, String currency) {
        return new PspCallbackOrder(
                1L,
                10L,
                20L,
                30L,
                "app",
                40L,
                pspCode,
                50L,
                "secret",
                "PO202406100001",
                "M202406100001",
                "PSP202406100001",
                "PROCESSING",
                new BigDecimal(amount),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(amount),
                currency,
                "https://merchant.example/notify"
        );
    }
}
