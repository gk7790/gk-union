package com.gk.psp.adapter.world;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.JSONWriter;
import com.gk.common.utils.BizKeyUtils;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.psp.adapter.PspPayAdapter;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.route.PspRouteResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class WorldPspSubmitAdapter implements PspPayAdapter, PspPayoutAdapter {
    private static final String HEADERS_JSON = "{\"Content-Type\":\"application/x-www-form-urlencoded\"}";
    private volatile RestClient restClient;

    @Override
    public boolean supports(String pspCode) {
        return StringUtils.isNotBlank(pspCode)
                && StringUtils.containsAnyIgnoreCase(pspCode, "WORLD", "WP001");
    }

    @Override
    public PspPayDispatchResult createPayOrder(PayOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = payParams(order, route);
        String path = "/open-api/create-pay-order";
        PspPayDispatchResult result = basePayResult(order, route, path, params);
        JSONObject response = post(route.getPspBaseUrl(), path, params, route.getPspAccountApiSecret());
        fillPayCreate(result, response, route.getPspAccountApiSecret());
        return result;
    }

    @Override
    public PspPayoutDispatchResult createPayoutOrder(PayoutOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = payoutParams(order, route);
        String path = "/open-api/create-payout-order";
        PspPayoutDispatchResult result = basePayoutResult(order, route, path, params);
        JSONObject response = post(route.getPspBaseUrl(), path, params, route.getPspAccountApiSecret());
        fillPayoutCreate(result, response, route.getPspAccountApiSecret());
        return result;
    }

    @Override
    public PspOrderQueryResult queryPayOrder(PayOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = queryParams(order.getPspOrderNo(), order.getPayOrderNo(), route);
        String path = "/open-api/query-pay-order";
        JSONObject response = post(route.getPspBaseUrl(), path, params, route.getPspAccountApiSecret());
        return buildQueryResult(order.getPayOrderNo(), order.getMerchantOrderNo(), order.getAmount(),
                order.getCurrency(), order.getPspOrderNo(), route, path, params, response, true);
    }

    @Override
    public PspOrderQueryResult queryPayoutOrder(PayoutOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = queryParams(order.getPspOrderNo(), order.getPayoutOrderNo(), route);
        String path = "/open-api/query-payout-order";
        JSONObject response = post(route.getPspBaseUrl(), path, params, route.getPspAccountApiSecret());
        return buildQueryResult(order.getPayoutOrderNo(), order.getMerchantOrderNo(), order.getAmount(),
                order.getCurrency(), order.getPspOrderNo(), route, path, params, response, false);
    }

    private Map<String, Object> payParams(PayOrderEntity order, PspRouteResult route) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", route.getPspAccountApiKey());
        params.put("merchant_order_id", order.getPayOrderNo());
        params.put("amount", amount(order.getAmount()));
        params.put("pay_channel", channel(route, order.getMethodCode()));
        params.put("notify_url", order.getNotifyUrl());
        params.put("page_return_url", order.getReturnUrl());
        mergeJson(params, order.getPayerJson());
        mergeJson(params, order.getExtraJson());
        return params;
    }

    private Map<String, Object> payoutParams(PayoutOrderEntity order, PspRouteResult route) {
        Map<String, Object> extra = jsonMap(order.getExtraJson());
        Map<String, Object> payee = jsonMap(order.getPayeeJson());
        String accountNo = firstText(extra, payee, "customer_account_no", "account_no");
        if (StringUtils.isBlank(accountNo)) {
            throw new IllegalStateException("World PSP payout requires customer_account_no in order extra");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", route.getPspAccountApiKey());
        params.put("merchant_order_id", order.getPayoutOrderNo());
        params.put("amount", amount(order.getAmount()));
        params.put("payout_mode", channel(route, order.getMethodCode()));
        params.put("customer_account_no", accountNo);
        params.put("notify_url", order.getNotifyUrl());
        putIfBlank(params, "customer_name", firstText(extra, payee, "customer_name", "name", order.getPayeeName()));
        putIfBlank(params, "customer_account_type", firstText(extra, payee, "customer_account_type", "wallet_type", order.getPayeeWalletType()));
        mergeJson(params, payee);
        mergeJson(params, extra);
        params.remove("account_no");
        params.remove("name");
        params.remove("wallet_type");
        params.remove("account_mask");
        params.remove("phone_mask");
        params.remove("email_mask");
        return params;
    }

    private Map<String, Object> queryParams(String pspOrderNo, String platformOrderNo, PspRouteResult route) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", route.getPspAccountApiKey());
        if (StringUtils.isNotBlank(pspOrderNo)) {
            params.put("system_order_id", pspOrderNo);
        } else {
            params.put("merchant_order_id", platformOrderNo);
        }
        return params;
    }

    private JSONObject post(String baseUrl, String path, Map<String, Object> params, String secret) {
        Map<String, Object> signed = WorldPspSignUtils.withSign(params, secret);
        String url = StringUtils.removeEnd(StringUtils.trimToEmpty(baseUrl), "/") + path;
        try {
            String body = restClient().post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(WorldPspSignUtils.formBody(signed))
                    .retrieve()
                    .body(String.class);
            return parseBody(body);
        } catch (RestClientResponseException ex) {
            return parseBody(ex.getResponseBodyAsString());
        }
    }

    private RestClient restClient() {
        RestClient current = restClient;
        if (current == null) {
            synchronized (this) {
                current = restClient;
                if (current == null) {
                    current = RestClient.create();
                    restClient = current;
                }
            }
        }
        return current;
    }

    private JSONObject parseBody(String body) {
        if (StringUtils.isBlank(body)) {
            JSONObject empty = new JSONObject();
            empty.put("code", 0);
            empty.put("message", "empty response");
            return empty;
        }
        return JSON.parseObject(body);
    }

    private PspPayDispatchResult basePayResult(PayOrderEntity order, PspRouteResult route, String path, Map<String, Object> params) {
        PspPayDispatchResult result = new PspPayDispatchResult();
        result.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        result.setRequestUrl(StringUtils.removeEnd(route.getPspBaseUrl(), "/") + path);
        result.setHttpMethod("POST");
        result.setRequestHeadersJson(HEADERS_JSON);
        result.setRequestBody(WorldPspSignUtils.formBody(WorldPspSignUtils.withSign(params, route.getPspAccountApiSecret())));
        result.setPspMerchantOrderNo(order.getPayOrderNo());
        return result;
    }

    private PspPayoutDispatchResult basePayoutResult(PayoutOrderEntity order, PspRouteResult route, String path, Map<String, Object> params) {
        PspPayoutDispatchResult result = new PspPayoutDispatchResult();
        result.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        result.setRequestUrl(StringUtils.removeEnd(route.getPspBaseUrl(), "/") + path);
        result.setHttpMethod("POST");
        result.setRequestHeadersJson(HEADERS_JSON);
        result.setRequestBody(WorldPspSignUtils.formBody(WorldPspSignUtils.withSign(params, route.getPspAccountApiSecret())));
        result.setPspMerchantOrderNo(order.getPayoutOrderNo());
        return result;
    }

    private void fillPayCreate(PspPayDispatchResult result, JSONObject response, String secret) {
        result.setResponseStatus(200);
        result.setResponseCode(response.getString("code"));
        result.setResponseMessage(response.getString("message"));
        result.setRawResponseJson(response.toJSONString());
        if (response.getIntValue("code") != 200) {
            result.setSuccess(false);
            result.setErrorCode(result.getResponseCode());
            result.setErrorMessage(result.getResponseMessage());
            return;
        }
        JSONObject data = response.getJSONObject("data");
        if (data == null || !WorldPspSignUtils.verify(data, secret, data.getString("sign"))) {
            result.setSuccess(false);
            result.setErrorCode("INVALID_SIGN");
            result.setErrorMessage("World PSP response signature invalid");
            return;
        }
        result.setSuccess(true);
        result.setPspOrderNo(data.getString("system_order_id"));
        result.setPayUrl(data.getString("pay_url"));
        result.setResponseSign(data.getString("sign"));
        result.setRawStatus(data.getString("order_status"));
        Map<String, Object> extras = new LinkedHashMap<>();
        putIfPresent(extras, "payQrCode", data.getString("payQrCode"));
        putIfPresent(extras, "clabe", data.getString("clabe"));
        putIfPresent(extras, "bank_code", data.getString("bank_code"));
        if (!extras.isEmpty()) {
            result.setPayParamsJson(JSON.toJSONString(extras, JSONWriter.Feature.WriteMapNullValue));
        }
    }

    private void fillPayoutCreate(PspPayoutDispatchResult result, JSONObject response, String secret) {
        result.setResponseStatus(200);
        result.setResponseCode(response.getString("code"));
        result.setResponseMessage(response.getString("message"));
        result.setRawResponseJson(response.toJSONString());
        if (response.getIntValue("code") != 200) {
            result.setSuccess(false);
            result.setErrorCode(result.getResponseCode());
            result.setErrorMessage(result.getResponseMessage());
            return;
        }
        JSONObject data = response.getJSONObject("data");
        if (data == null || !WorldPspSignUtils.verify(data, secret, data.getString("sign"))) {
            result.setSuccess(false);
            result.setErrorCode("INVALID_SIGN");
            result.setErrorMessage("World PSP response signature invalid");
            return;
        }
        result.setSuccess(true);
        result.setPspOrderNo(data.getString("system_order_id"));
        result.setResponseSign(data.getString("sign"));
        result.setRawStatus(StringUtils.defaultIfBlank(data.getString("order_status"), PayoutOrderStatusEnum.PROCESSING.code()));
    }

    private PspOrderQueryResult buildQueryResult(String systemOrderNo,
                                                 String merchantOrderNo,
                                                 BigDecimal orderAmount,
                                                 String currency,
                                                 String pspOrderNo,
                                                 PspRouteResult route,
                                                 String path,
                                                 Map<String, Object> params,
                                                 JSONObject response,
                                                 boolean payOrder) {
        PspOrderQueryResult.PspOrderQueryResultBuilder builder = PspOrderQueryResult.builder()
                .pspCode(route.getPspCode())
                .systemOrderNo(systemOrderNo)
                .merchantOrderNo(merchantOrderNo)
                .currency(currency)
                .pspRequestNo(BizKeyUtils.genPspRequestNo())
                .requestUrl(StringUtils.removeEnd(route.getPspBaseUrl(), "/") + path)
                .httpMethod("POST")
                .requestHeadersJson(HEADERS_JSON)
                .requestBody(WorldPspSignUtils.formBody(WorldPspSignUtils.withSign(params, route.getPspAccountApiSecret())))
                .responseStatus(200)
                .responseCode(response.getString("code"))
                .responseMessage(response.getString("message"))
                .rawResponseJson(response.toJSONString());

        if (response.getIntValue("code") != 200) {
            return builder.success(false).errorCode(response.getString("code")).errorMessage(response.getString("message")).build();
        }
        JSONObject data = response.getJSONObject("data");
        if (data == null || !WorldPspSignUtils.verify(data, route.getPspAccountApiSecret(), data.getString("sign"))) {
            return builder.success(false).errorCode("INVALID_SIGN").errorMessage("World PSP response signature invalid").build();
        }
        String pspStatus = data.getString("order_status");
        BigDecimal amount = decimal(data.getString("amount"), orderAmount);
        return builder
                .success(true)
                .pspOrderNo(StringUtils.defaultIfBlank(data.getString("system_order_id"), pspOrderNo))
                .pspStatus(pspStatus)
                .orderStatus(payOrder ? toPayStatus(pspStatus) : toPayoutStatus(pspStatus))
                .amount(amount)
                .responseSign(data.getString("sign"))
                .build();
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

    private void mergeJson(Map<String, Object> target, String json) {
        mergeJson(target, jsonMap(json));
    }

    private void mergeJson(Map<String, Object> target, Map<String, Object> source) {
        source.forEach((key, value) -> {
            if (StringUtils.isBlank(key) || value == null || "sign".equalsIgnoreCase(key)) {
                return;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty() && !target.containsKey(key)) {
                target.put(key, text);
            }
        });
    }

    private Map<String, Object> jsonMap(String json) {
        if (StringUtils.isBlank(json)) {
            return Map.of();
        }
        try {
            Map<String, Object> map = JSON.parseObject(json, new TypeReference<Map<String, Object>>() {
            });
            return map == null ? Map.of() : map;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String channel(PspRouteResult route, String fallback) {
        return StringUtils.defaultIfBlank(route.getPspMethodCode(), fallback);
    }

    private String amount(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String firstText(Map<String, Object> first, Map<String, Object> second, String... keys) {
        for (String key : keys) {
            String value = text(first.get(key));
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
            value = text(second.get(key));
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private void putIfBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private String text(Object value) {
        return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
    }

    private BigDecimal decimal(String value, BigDecimal fallback) {
        return StringUtils.isBlank(value) ? fallback : new BigDecimal(value);
    }
}
