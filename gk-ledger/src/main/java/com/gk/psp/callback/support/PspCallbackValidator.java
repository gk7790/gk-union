package com.gk.psp.callback.support;

import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PspCallbackValidator {
    public void validateTerminalCallback(String bizType, PspCallbackResult result, PspCallbackOrder order) {
        if (result == null || order == null) {
            throw new IllegalStateException("Invalid PSP callback context");
        }
        if (StringUtils.isNotBlank(result.getPspCode())
                && StringUtils.isNotBlank(order.pspCode())
                && !StringUtils.equalsIgnoreCase(result.getPspCode(), order.pspCode())) {
            throw new IllegalStateException("PSP callback PSP code mismatch");
        }
        String status = PspCallbackUtils.normalizeStatus(result.getOrderStatus());
        if (PspCallbackConstants.STATUS_SUCCESS.equals(status)) {
            validateRequiredAmount(result.getAmount(), order.amount());
            validateRequiredCurrency(result.getCurrency(), order.currency());
            return;
        }
        validateOptionalAmount(result.getAmount(), order.amount());
        validateOptionalCurrency(result.getCurrency(), order.currency());
    }

    private void validateRequiredAmount(BigDecimal callbackAmount, BigDecimal orderAmount) {
        if (callbackAmount == null || orderAmount == null || callbackAmount.compareTo(orderAmount) != 0) {
            throw new IllegalStateException("PSP callback amount mismatch");
        }
    }

    private void validateOptionalAmount(BigDecimal callbackAmount, BigDecimal orderAmount) {
        if (callbackAmount != null && orderAmount != null && callbackAmount.compareTo(orderAmount) != 0) {
            throw new IllegalStateException("PSP callback amount mismatch");
        }
    }

    private void validateRequiredCurrency(String callbackCurrency, String orderCurrency) {
        if (StringUtils.isBlank(callbackCurrency) || !StringUtils.equalsIgnoreCase(callbackCurrency, orderCurrency)) {
            throw new IllegalStateException("PSP callback currency mismatch");
        }
    }

    private void validateOptionalCurrency(String callbackCurrency, String orderCurrency) {
        if (StringUtils.isNotBlank(callbackCurrency) && !StringUtils.equalsIgnoreCase(callbackCurrency, orderCurrency)) {
            throw new IllegalStateException("PSP callback currency mismatch");
        }
    }
}
