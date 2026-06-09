package com.gk.psp.adapter.world;

import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.adapter.PspPayAdapter;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.route.PspRouteResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WorldPspSubmitAdapter implements PspPayAdapter, PspPayoutAdapter {
    private static final String PSP_PAY_ORDER_NO = "pay_27529953536";
    private static final String PSP_PAYOUT_ORDER_NO = "payout_27529953536";

    @Override
    public boolean supports(String pspCode) {
        if (StringUtils.isBlank(pspCode)) {
            return false;
        }
        return StringUtils.containsAnyIgnoreCase(pspCode, "WORLD", "WP001");
    }

    @Override
    public PspPayDispatchResult createPayOrder(PayOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("merchant_order_id", order.getPayOrderNo());
        params.put("amount", order.getAmount());
        params.put("currency", order.getCurrency());
        params.put("pay_channel", order.getMethodCode());
        params.put("notify_url", order.getNotifyUrl());
        params.put("sign", WorldPspSignUtils.sign(params, route.getPspAccountApiSecret()));

        PspPayDispatchResult result = new PspPayDispatchResult();
        result.setSuccess(true);
        result.setPspRequestNo("PRQ_DEMO_WORLD_PAY");
        result.setRequestUrl(defaultBaseUrl(route) + "/open-api/create-pay-order");
        result.setHttpMethod("POST");
        result.setRequestHeadersJson("{\"Content-Type\":\"application/x-www-form-urlencoded\"}");
        result.setRequestBody(WorldPspSignUtils.canonicalText(params) + "&sign=" + params.get("sign"));
        result.setResponseStatus(200);
        result.setPspMerchantOrderNo(order.getPayOrderNo());
        result.setPayUrl(defaultBaseUrl(route) + "/payment-page?param=fffN1BidnFNTzRSRj....");
        result.setPspOrderNo(PSP_PAY_ORDER_NO);
        result.setRawStatus("SUCCESS");
        result.setResponseCode("200");
        result.setResponseMessage("success");
        result.setResponseSign("0a8b3c95541e092a63577724b9c66e24");
        result.setRawResponseJson("{\"code\":200,\"message\":\"success\",\"data\":{\"system_order_id\":\""
                + PSP_PAY_ORDER_NO + "\",\"merchant_order_id\":\"" + order.getPayOrderNo()
                + "\",\"pay_url\":\"" + result.getPayUrl() + "\"}}");
        return result;
    }

    @Override
    public PspPayoutDispatchResult createPayoutOrder(PayoutOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("merchant_order_id", order.getPayoutOrderNo());
        params.put("amount", order.getAmount());
        params.put("currency", order.getCurrency());
        params.put("payout_mode", order.getMethodCode());
        params.put("account_no", order.getPayeeAccountMask());
        params.put("notify_url", order.getNotifyUrl());
        params.put("sign", WorldPspSignUtils.sign(params, route.getPspAccountApiSecret()));

        PspPayoutDispatchResult result = new PspPayoutDispatchResult();
        result.setSuccess(true);
        result.setPspRequestNo("PRQ_DEMO_WORLD_PAYOUT");
        result.setRequestUrl(defaultBaseUrl(route) + "/open-api/create-payout-order");
        result.setHttpMethod("POST");
        result.setRequestHeadersJson("{\"Content-Type\":\"application/x-www-form-urlencoded\"}");
        result.setRequestBody(WorldPspSignUtils.canonicalText(params) + "&sign=" + params.get("sign"));
        result.setResponseStatus(200);
        result.setPspMerchantOrderNo(order.getPayoutOrderNo());
        result.setPspOrderNo(PSP_PAYOUT_ORDER_NO);
        result.setRawStatus("PROCESSING");
        result.setResponseCode("200");
        result.setResponseMessage("success");
        result.setResponseSign("0a8b3c95541e092a63577724b9c66e24");
        result.setRawResponseJson("{\"code\":200,\"message\":\"success\",\"data\":{\"system_order_id\":\""
                + PSP_PAYOUT_ORDER_NO + "\",\"merchant_order_id\":\"" + order.getPayoutOrderNo() + "\"}}");
        return result;
    }

    private String defaultBaseUrl(PspRouteResult route) {
        return StringUtils.defaultIfBlank(route.getPspBaseUrl(), "https://XXXX");
    }
}
