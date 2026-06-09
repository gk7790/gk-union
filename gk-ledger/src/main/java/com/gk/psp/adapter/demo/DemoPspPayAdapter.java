package com.gk.psp.adapter.demo;

import com.gk.common.utils.BizKeyUtils;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.psp.adapter.PspPayAdapter;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.route.PspRouteResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DemoPspPayAdapter implements PspPayAdapter {
    private static final String DEFAULT_PAY_BASE_URL = "https://psp-demo.example.com";

    @Override
    public boolean supports(String pspCode) {
        if (StringUtils.isBlank(pspCode)) {
            return false;
        }
        String normalized = pspCode.toUpperCase(Locale.ROOT);
        return "DEMO".equals(normalized) || "DEMO_PSP".equals(normalized) || "PH_DEMO_PSP".equals(normalized);
    }

    @Override
    public PspPayDispatchResult createPayOrder(PayOrderEntity order, PspRouteResult route) {
        String baseUrl = StringUtils.defaultIfBlank(route.getPspBaseUrl(), DEFAULT_PAY_BASE_URL);

        PspPayDispatchResult result = new PspPayDispatchResult();
        result.setSuccess(true);
        result.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        result.setRequestUrl(baseUrl + "/open-api/create-pay-order");
        result.setHttpMethod("POST");
        result.setRequestHeadersJson("{\"Content-Type\":\"application/json\"}");
        result.setRequestBody("{\"merchant_order_id\":\"" + order.getPayOrderNo() + "\",\"amount\":\""
                + order.getAmount() + "\",\"currency\":\"" + order.getCurrency() + "\"}");
        result.setResponseStatus(200);
        result.setPspOrderNo("PSP" + order.getPayOrderNo());
        result.setPspMerchantOrderNo(order.getPayOrderNo());
        result.setPayUrl(baseUrl + "/pay/" + order.getPayOrderNo());
        result.setRawStatus("PROCESSING");
        result.setResponseCode("200");
        result.setResponseMessage("success");
        result.setRawResponseJson("{\"code\":200,\"message\":\"success\"}");
        return result;
    }
}
