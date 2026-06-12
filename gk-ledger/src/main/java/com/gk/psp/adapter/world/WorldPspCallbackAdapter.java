package com.gk.psp.adapter.world;

import com.gk.payment.enums.PayOrderStatusEnum;
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
        if (StringUtils.isBlank(pspCode)) {
            return false;
        }
        return StringUtils.containsAnyIgnoreCase(pspCode, "WORLD", "WP001");
    }

    @Override
    public PspCallbackResult parsePayCallback(PspCallbackRequest request) {
        return parse(request);
    }

    @Override
    public PspCallbackResult parsePayoutCallback(PspCallbackRequest request) {
        return parse(request);
    }

    @Override
    public boolean verifySign(PspCallbackRequest request) {
        return StringUtils.isNotBlank(request.getApiSecret())
                && WorldPspSignUtils.verify(request.getParams(), request.getApiSecret(), text(request.getParams(), "sign", "signature"));
    }

    private PspCallbackResult parse(PspCallbackRequest request) {
        Map<String, Object> params = request.getParams();
        PspCallbackResult result = new PspCallbackResult();
        result.setPspCode(request.getPspCode());
        result.setBizType(request.getBizType());
        result.setSystemOrderNo(text(params, "merchant_order_id", "pay_order_no", "payout_order_no"));
        result.setMerchantOrderNo(text(params, "merchant_order_no"));
        result.setPspOrderNo(text(params, "system_order_id", "psp_order_no", "order_id", "transaction_id"));
        result.setPspStatus(text(params, "status", "order_status", "trade_status"));
        result.setOrderStatus(toOrderStatus(result.getPspStatus()));
        result.setAmount(decimal(params, "amount", "paid_amount"));
        result.setCurrency(text(params, "currency"));
        result.setCallbackId(text(params, "callback_id", "notify_id"));
        result.setCallbackType(StringUtils.defaultIfBlank(text(params, "callback_type", "event_type"), request.getBizType()));
        result.setSignature(text(params, "sign", "signature"));
        result.setErrorCode(text(params, "error_code", "fail_code"));
        result.setErrorMessage(text(params, "error_message", "fail_msg", "message"));
        result.setSuccessResponse("success");
        result.setFailResponse("fail");
        return result;
    }

    private String toOrderStatus(String pspStatus) {
        String normalized = StringUtils.defaultString(pspStatus).trim().toUpperCase(Locale.ROOT);
        if (StringUtils.equalsAny(normalized, "SUCCESS", "SUCCEEDED", "PAID", "COMPLETED", "DONE")) {
            return PayOrderStatusEnum.SUCCESS.code();
        }
        if (StringUtils.equalsAny(normalized, "FAILED", "FAIL", "CLOSED", "CANCELLED", "REJECTED")) {
            return PayOrderStatusEnum.FAILED.code();
        }
        return PayOrderStatusEnum.PROCESSING.code();
    }

    private BigDecimal decimal(Map<String, Object> params, String... names) {
        String value = text(params, names);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new BigDecimal(value);
    }

    private String text(Map<String, Object> params, String... names) {
        if (params == null || names == null) {
            return null;
        }
        for (String name : names) {
            Object value = params.get(name);
            if (value == null) {
                continue;
            }
            String text = StringUtils.trimToNull(String.valueOf(value));
            if (text != null) {
                return text;
            }
        }
        return null;
    }
}
