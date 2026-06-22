package com.gk.psp.adapter.demo;

import com.gk.common.utils.BizKeyUtils;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.query.PspOrderQueryResult;
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
    public PspPayoutDispatchResult createPayoutOrder(PayoutOrderEntity order, PspRouteResult route) {
        String baseUrl = StringUtils.defaultIfBlank(route.getPspBaseUrl(), DEFAULT_BASE_URL);

        PspPayoutDispatchResult result = new PspPayoutDispatchResult();
        result.setSuccess(true);
        result.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        result.setRequestUrl(baseUrl + "/open-api/create-payout-order");
        result.setHttpMethod("POST");
        result.setRequestHeadersJson("{\"Content-Type\":\"application/json\"}");
        result.setRequestBody("{\"merchant_order_id\":\"" + order.getPayoutOrderNo() + "\",\"amount\":\""
                + order.getAmount() + "\",\"currency\":\"" + order.getCurrency()
                + "\",\"account_no\":\"" + StringUtils.defaultString(order.getPayeeAccountMask())
                + "\",\"bank_code\":\"" + StringUtils.defaultIfBlank(route.getPspBankCode(), order.getPayeeBankCode()) + "\"}");
        result.setResponseStatus(200);
        result.setPspOrderNo("PSP" + order.getPayoutOrderNo());
        result.setPspMerchantOrderNo(order.getPayoutOrderNo());
        result.setRawStatus("PROCESSING");
        result.setResponseCode("200");
        result.setResponseMessage("success");
        result.setRawResponseJson("{\"code\":200,\"message\":\"success\"}");
        return result;
    }

    @Override
    public PspOrderQueryResult queryPayoutOrder(PayoutOrderEntity order, PspRouteResult route) {
        String baseUrl = StringUtils.defaultIfBlank(route.getPspBaseUrl(), DEFAULT_BASE_URL);
        String pspOrderNo = StringUtils.defaultIfBlank(order.getPspOrderNo(), "PSP" + order.getPayoutOrderNo());
        return PspOrderQueryResult.builder()
                .success(true)
                .pspCode(route.getPspCode())
                .systemOrderNo(order.getPayoutOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .pspOrderNo(pspOrderNo)
                .pspStatus(PayoutOrderStatusEnum.PROCESSING.code())
                .orderStatus(PayoutOrderStatusEnum.PROCESSING.code())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .pspRequestNo(BizKeyUtils.genPspRequestNo())
                .requestUrl(baseUrl + "/open-api/query-payout-order")
                .httpMethod("POST")
                .requestHeadersJson("{\"Content-Type\":\"application/json\"}")
                .requestBody("{\"merchant_order_id\":\"" + order.getPayoutOrderNo() + "\"}")
                .responseStatus(200)
                .responseCode("200")
                .responseMessage("success")
                .rawResponseJson("{\"code\":200,\"message\":\"success\",\"status\":\"PROCESSING\"}")
                .build();
    }
}
