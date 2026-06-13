package com.gk.psp.adapter.world;

import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.payout.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.payout.enums.PayoutOrderStatusEnum;
import com.gk.psp.adapter.PspPayAdapter;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.query.PspOrderQueryResult;
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

    @Override
    public PspOrderQueryResult queryPayOrder(PayOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("merchant_order_id", order.getPayOrderNo());
        params.put("system_order_id", StringUtils.defaultIfBlank(order.getPspOrderNo(), PSP_PAY_ORDER_NO));
        params.put("sign", WorldPspSignUtils.sign(params, route.getPspAccountApiSecret()));

        return PspOrderQueryResult.builder()
                .success(true)
                .pspCode(route.getPspCode())
                .systemOrderNo(order.getPayOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .pspOrderNo(StringUtils.defaultIfBlank(order.getPspOrderNo(), PSP_PAY_ORDER_NO))
                .pspStatus(PayOrderStatusEnum.PROCESSING.code())
                .orderStatus(PayOrderStatusEnum.PROCESSING.code())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .pspRequestNo("PRQ_DEMO_WORLD_PAY_QUERY")
                .requestUrl(defaultBaseUrl(route) + "/open-api/query-pay-order")
                .httpMethod("POST")
                .requestHeadersJson("{\"Content-Type\":\"application/x-www-form-urlencoded\"}")
                .requestBody(WorldPspSignUtils.canonicalText(params) + "&sign=" + params.get("sign"))
                .responseStatus(200)
                .responseCode("200")
                .responseMessage("success")
                .responseSign("0a8b3c95541e092a63577724b9c66e24")
                .rawResponseJson("{\"code\":200,\"message\":\"success\",\"data\":{\"system_order_id\":\""
                        + StringUtils.defaultIfBlank(order.getPspOrderNo(), PSP_PAY_ORDER_NO)
                        + "\",\"merchant_order_id\":\"" + order.getPayOrderNo()
                        + "\",\"status\":\"PROCESSING\"}}")
                .build();
    }

    @Override
    public PspOrderQueryResult queryPayoutOrder(PayoutOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("merchant_order_id", order.getPayoutOrderNo());
        params.put("system_order_id", StringUtils.defaultIfBlank(order.getPspOrderNo(), PSP_PAYOUT_ORDER_NO));
        params.put("sign", WorldPspSignUtils.sign(params, route.getPspAccountApiSecret()));

        return PspOrderQueryResult.builder()
                .success(true)
                .pspCode(route.getPspCode())
                .systemOrderNo(order.getPayoutOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .pspOrderNo(StringUtils.defaultIfBlank(order.getPspOrderNo(), PSP_PAYOUT_ORDER_NO))
                .pspStatus(PayoutOrderStatusEnum.PROCESSING.code())
                .orderStatus(PayoutOrderStatusEnum.PROCESSING.code())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .pspRequestNo("PRQ_DEMO_WORLD_PAYOUT_QUERY")
                .requestUrl(defaultBaseUrl(route) + "/open-api/query-payout-order")
                .httpMethod("POST")
                .requestHeadersJson("{\"Content-Type\":\"application/x-www-form-urlencoded\"}")
                .requestBody(WorldPspSignUtils.canonicalText(params) + "&sign=" + params.get("sign"))
                .responseStatus(200)
                .responseCode("200")
                .responseMessage("success")
                .responseSign("0a8b3c95541e092a63577724b9c66e24")
                .rawResponseJson("{\"code\":200,\"message\":\"success\",\"data\":{\"system_order_id\":\""
                        + StringUtils.defaultIfBlank(order.getPspOrderNo(), PSP_PAYOUT_ORDER_NO)
                        + "\",\"merchant_order_id\":\"" + order.getPayoutOrderNo()
                        + "\",\"status\":\"PROCESSING\"}}")
                .build();
    }

    private String defaultBaseUrl(PspRouteResult route) {
        return StringUtils.defaultIfBlank(route.getPspBaseUrl(), "https://XXXX");
    }
}
