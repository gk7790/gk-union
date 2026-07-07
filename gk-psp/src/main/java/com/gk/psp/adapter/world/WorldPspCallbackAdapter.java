package com.gk.psp.adapter.world;

import com.gk.common.model.Result;
import com.gk.psp.adapter.PspCallbackAdapter;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.callback.support.PspCallbackAckMapper;
import com.gk.psp.callback.support.PspCallbackStatus;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Component
public class WorldPspCallbackAdapter implements PspCallbackAdapter {
    @Override
    public boolean supports(String pspCode) {
        String normalized = StringUtils.upperCase(pspCode, Locale.ROOT);
        return StringUtils.isNotBlank(normalized)
                && (normalized.contains("WORLD") || normalized.contains("WP001"));
    }

    @Override
    public Result<PspCallbackResult> parsePayCallback(PspCallbackRequest request) {
        return parse(request, true);
    }

    @Override
    public Result<PspCallbackResult> parsePayoutCallback(PspCallbackRequest request) {
        return parse(request, false);
    }

    @Override
    public Result<Void> verifySign(PspCallbackRequest request) {
        String apiSecret = request.getApiSecret();
        if (StringUtils.isBlank(apiSecret)) {
            return Result.fail(PspCallbackAckMapper.SIGN_INVALID);
        }
        String signature = text(request.getParams(), "sign");
        if (StringUtils.isBlank(signature)) {
            return Result.fail(PspCallbackAckMapper.SIGN_INVALID);
        }
        if (WorldPspSignUtils.notVerify(request.getParams(), apiSecret, signature)) {
            return Result.fail(PspCallbackAckMapper.SIGN_INVALID);
        }
        return Result.success(null);
    }

    private Result<PspCallbackResult> parse(PspCallbackRequest request, boolean payinOrder) {
        try {
            Map<String, Object> params = request.getParams();
            String pspStatus = text(params, "order_status", "status");

            PspCallbackResult result = new PspCallbackResult();
            result.setPspCode(request.getPspCode());
            result.setBizType(request.getBizType());
            result.setSystemOrderNo(text(params, "merchant_order_id"));
            result.setMerchantOrderNo(text(params, "merchant_order_no"));
            result.setPspOrderNo(text(params, "system_order_id"));
            result.setPspStatus(pspStatus);
            result.setOrderStatus(payinOrder ? toPayStatus(pspStatus) : toPayoutStatus(pspStatus));
            result.setAmount(decimal(params, "amount"));
            result.setCurrency(text(params, "currency"));
            result.setSignature(text(params, "sign"));
            result.setErrorMessage(text(params, "msg", "message"));
            result.setSuccessResponse("success");
            result.setFailResponse("fail");
            return Result.success(result);
        } catch (Exception ex) {
            return Result.fail(PspCallbackAckMapper.PARSE_FAILED);
        }
    }

    private String toPayStatus(String status) {
        String value = StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PAYIN_SUCCESS", "PAY_SUCCESS", "SUCCESS", "PAID", "COMPLETED" -> PspCallbackStatus.SUCCESS.code();
            case "PAYIN_FAILED", "PAY_FAILED", "PAY_FAIL", "FAILED", "CLOSED", "CANCELLED" -> PspCallbackStatus.FAILED.code();
            default -> PspCallbackStatus.PROCESSING.code();
        };
    }

    private String toPayoutStatus(String status) {
        String value = StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PAYIN_SUCCESS", "PAY_SUCCESS", "SUCCESS", "COMPLETED" -> PspCallbackStatus.SUCCESS.code();
            case "PAYIN_FAILED", "PAY_FAILED", "PAY_FAIL", "FAILED", "REJECTED" -> PspCallbackStatus.FAILED.code();
            case "CANCELLED", "CANCELED" -> PspCallbackStatus.CANCELLED.code();
            default -> PspCallbackStatus.PROCESSING.code();
        };
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
