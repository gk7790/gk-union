package com.gk.psp.adapter.demo;

import com.gk.payment.domain.key.BizKeyUtils;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.callback.support.PspCallbackStatus;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.enums.PspPayoutSubmitStatus;

import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DemoPspPayoutAdapter implements PspPayoutAdapter {
    private static final String DEFAULT_BASE_URL = "https://psp-demo.example.com";

    @Override
    public boolean supports(String pspCode) {
        if (StringUtils.isBlank(pspCode)) {
            return false;
        }
        String normalized = pspCode.toUpperCase(Locale.ROOT);
        return "DEMO".equals(normalized) || "DEMO_PSP".equals(normalized) || "PH_DEMO_PSP".equals(normalized);
    }

    @Override
    public PspPayoutDispatchResult createPayoutOrder(PspOrderRequest order, PspRouteResult route) {
        String baseUrl = StringUtils.defaultIfBlank(route.getPspBaseUrl(), DEFAULT_BASE_URL);

        PspPayoutDispatchResult result = new PspPayoutDispatchResult();
        result.setSuccess(true);
        result.setSubmitResultStatus(PspPayoutSubmitStatus.ACCEPTED);
        result.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        result.setRequestUrl(baseUrl + "/open-api/create-payout-order");
        result.setHttpMethod("POST");
        result.setRequestHeadersJson("{\"Content-Type\":\"application/json\"}");
        result.setRequestBody("{\"merchant_order_id\":\"" + order.getOrderNo() + "\",\"amount\":\""
                + order.getAmount() + "\",\"currency\":\"" + order.getCurrency()
                + "\",\"account_no\":\"" + StringUtils.defaultString(order.getPayeeAccountNo())
                + "\",\"bank_code\":\"" + StringUtils.defaultIfBlank(route.getPspBankCode(), order.getPayeeBankCode()) + "\"}");
        result.setResponseStatus(200);
        result.setPspOrderNo("PSP" + order.getOrderNo());
        result.setPspMerchantOrderNo(order.getOrderNo());
        result.setRawStatus("PROCESSING");
        result.setResponseCode("200");
        result.setResponseMessage("success");
        result.setRawResponseJson("{\"code\":200,\"message\":\"success\"}");
        return result;
    }

    @Override
    public PspOrderQueryResult queryPayoutOrder(PspOrderRequest order, PspRouteResult route) {
        String baseUrl = StringUtils.defaultIfBlank(route.getPspBaseUrl(), DEFAULT_BASE_URL);
        String pspOrderNo = StringUtils.defaultIfBlank(order.getPspOrderNo(), "PSP" + order.getOrderNo());
        return PspOrderQueryResult.builder()
                .success(true)
                .pspCode(route.getPspCode())
                .systemOrderNo(order.getOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .pspOrderNo(pspOrderNo)
                .pspStatus(PspCallbackStatus.PROCESSING.code())
                .orderStatus(PspCallbackStatus.PROCESSING.code())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .pspRequestNo(BizKeyUtils.genPspRequestNo())
                .requestUrl(baseUrl + "/open-api/query-payout-order")
                .httpMethod("POST")
                .requestHeadersJson("{\"Content-Type\":\"application/json\"}")
                .requestBody("{\"merchant_order_id\":\"" + order.getOrderNo() + "\"}")
                .responseStatus(200)
                .responseCode("200")
                .responseMessage("success")
                .rawResponseJson("{\"code\":200,\"message\":\"success\",\"status\":\"PROCESSING\"}")
                .build();
    }
}
