package com.gk.psp.callback.support;

import com.gk.common.model.Result;
import com.gk.psp.callback.model.PspCallbackHandleResult;
import com.gk.psp.callback.model.PspCallbackResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PspCallbackAckMapper {
    public static final String ADAPTER_NOT_FOUND = "ADAPTER_NOT_FOUND";
    public static final String IP_FORBIDDEN = "IP_FORBIDDEN";
    public static final String PARSE_FAILED = "PARSE_FAILED";
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String SIGN_INVALID = "SIGN_INVALID";
    public static final String AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String CURRENCY_MISMATCH = "CURRENCY_MISMATCH";
    public static final String PSP_CODE_MISMATCH = "PSP_CODE_MISMATCH";
    public static final String UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    public PspCallbackResponse toResponse(Result<PspCallbackHandleResult> result, String failBody) {
        if (result != null && result.isSuccess()) {
            PspCallbackHandleResult data = result.getData();
            if (data != null && data.getResponse() != null) {
                return data.getResponse();
            }
            return PspCallbackResponse.ok(PspCallbackResponse.DEFAULT_SUCCESS_BODY);
        }
        String code = result == null ? SYSTEM_ERROR : StringUtils.defaultIfBlank(result.getMsg(), SYSTEM_ERROR);
        return switch (code) {
            case SIGN_INVALID -> PspCallbackResponse.unauthorized(failBody);
            case IP_FORBIDDEN -> PspCallbackResponse.forbidden(failBody);
            case ADAPTER_NOT_FOUND, PARSE_FAILED, ORDER_NOT_FOUND, AMOUNT_MISMATCH,
                    CURRENCY_MISMATCH, PSP_CODE_MISMATCH, UNSUPPORTED_STATUS ->
                    PspCallbackResponse.badRequest(failBody);
            default -> PspCallbackResponse.internalServerError(failBody);
        };
    }
}
