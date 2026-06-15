package com.gk.psp.adapter.world;

import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.psp.callback.adapter.PspCallbackAdapter;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Component
public class WorldPspCallbackAdapter implements PspCallbackAdapter {
    @Override
    public boolean supports(String pspCode) {
        return StringUtils.isNotBlank(pspCode)
                && StringUtils.containsAnyIgnoreCase(pspCode, "WORLD", "WP001");
    }

    @Override
    public PspCallbackResult parsePayCallback(PspCallbackRequest request) {
        return parse(request, true);
    }

    @Override
    public PspCallbackResult parsePayoutCallback(PspCallbackRequest request) {
        return parse(request, false);
    }

    @Override
    public boolean verifySign(PspCallbackRequest request) {
        return StringUtils.isNotBlank(request.getApiSecret())
                && WorldPspSignUtils.verify(request.getParams(), request.getApiSecret(), text(request.getParams(), "sign"));
    }

    private PspCallbackResult parse(PspCallbackRequest request, boolean payOrder) {
        Map<String, Object> params = request.getParams();
        String pspStatus = text(params, "order_status", "status");

        PspCallbackResult result = new PspCallbackResult();
        result.setPspCode(request.getPspCode());
        result.setBizType(request.getBizType());
        result.setSystemOrderNo(text(params, "merchant_order_id"));
        result.setMerchantOrderNo(text(params, "merchant_order_no"));
        result.setPspOrderNo(text(params, "system_order_id"));
        result.setPspStatus(pspStatus);
        result.setOrderStatus(payOrder ? toPayStatus(pspStatus) : toPayoutStatus(pspStatus));
        result.setAmount(decimal(params, "amount"));
        result.setCurrency(text(params, "currency"));
        result.setSignature(text(params, "sign"));
        result.setErrorMessage(text(params, "msg", "message"));
        result.setSuccessResponse("success");
        result.setFailResponse("fail");
        return result;
    }

    private String toPayStatus(String status) {
        String value = StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
        if (StringUtils.equalsAny(value, "PAY_SUCCESS", "SUCCESS", "PAID", "COMPLETED")) {
            return PayOrderStatusEnum.SUCCESS.code();
        }
        if (StringUtils.equalsAny(value, "PAY_FAILED", "FAILED", "CLOSED", "CANCELLED")) {
            return PayOrderStatusEnum.FAILED.code();
        }
        return PayOrderStatusEnum.PROCESSING.code();
    }

    private String toPayoutStatus(String status) {
        String value = StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
        if (StringUtils.equalsAny(value, "PAY_SUCCESS", "SUCCESS", "COMPLETED")) {
            return PayoutOrderStatusEnum.SUCCESS.code();
        }
        if (StringUtils.equalsAny(value, "PAY_FAILED", "FAILED", "REJECTED")) {
            return PayoutOrderStatusEnum.FAILED.code();
        }
        if (StringUtils.equalsAny(value, "CANCELLED", "CANCELED")) {
            return PayoutOrderStatusEnum.CANCELLED.code();
        }
        return PayoutOrderStatusEnum.PROCESSING.code();
    }

    private BigDecimal decimal(Map<String, Object> params, String... names) {
        String value = text(params, names);
        return StringUtils.isBlank(value) ? null : new BigDecimal(value);
    }

    private String text(Map<String, Object> params, String... names) {
        if (params == null) {
            return null;
        }
        for (String name : names) {
            Object value = params.get(name);
            if (value != null) {
                String text = StringUtils.trimToNull(String.valueOf(value));
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }
}
