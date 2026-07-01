package com.gk.psp.adapter.world;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.JSONWriter;
import com.gk.common.utils.BizKeyUtils;
import com.gk.psp.adapter.PspBalanceAdapter;
import com.gk.psp.adapter.PspPayAdapter;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.balance.PspBalanceSnap;
import com.gk.psp.callback.support.PspCallbackUtils;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.enums.PspPayoutSubmitResultStatus;

import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.request.PspBalanceRequest;
import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class WorldPspSubmitAdapter implements PspPayAdapter, PspPayoutAdapter, PspBalanceAdapter {
    private static final String HEADERS_JSON = "{\"Content-Type\":\"application/x-www-form-urlencoded\"}";
    private static final boolean MOCK_SUBMIT = false;
    // 下单需要同步拿 pay_url，超时要短而明确，避免PSP 长时间占用商户请求线程
    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 8_000;
    private volatile RestClient restClient;

    @Override
    public boolean supports(String pspCode) {
        if (StringUtils.isBlank(pspCode)) {
            return false;
        }
        String normalized = pspCode.toUpperCase(Locale.ROOT);
        return normalized.contains("WORLD") || normalized.contains("WP001");
    }

    @Override
    public PspPayDispatchResult createPayinOrder(PspOrderRequest order, PspRouteResult route) {
        requireRouteCredentials(route, "create payin order");
        Map<String, Object> params = payParams(order, route);
        String path = "/open-api/create-pay-order";
        PspPayDispatchResult result = basePayResult(order, route, path, params);
        if (MOCK_SUBMIT) {
            fillMockPayCreate(result, order);
            return result;
        }
        WorldPspHttpResponse response = post(route.getPspBaseUrl(), path, params, route.getPspAccountApiSecret());
        fillPayCreate(result, response, route.getPspAccountApiSecret());
        return result;
    }

    @Override
    public PspPayoutDispatchResult createPayoutOrder(PspOrderRequest order, PspRouteResult route) {
        requireRouteCredentials(route, "create payout order");
        Map<String, Object> params = payoutParams(order, route);
        String path = "/open-api/create-payout-order";
        PspPayoutDispatchResult result = basePayoutResult(order, route, path, params);
        if (MOCK_SUBMIT) {
            fillMockPayoutCreate(result, order);
            return result;
        }
        WorldPspHttpResponse response = post(route.getPspBaseUrl(), path, params, route.getPspAccountApiSecret());
        fillPayoutCreate(result, response, route.getPspAccountApiSecret());
        return result;
    }

    @Override
    public PspOrderQueryResult queryPayinOrder(PspOrderRequest order, PspRouteResult route) {
        requireRouteCredentials(route, "query payin order");
        Map<String, Object> params = queryParams(order.getPspOrderNo(), order.getOrderNo(), route);
        String path = "/open-api/query-pay-order";
        WorldPspHttpResponse response = post(route.getPspBaseUrl(), path, params, route.getPspAccountApiSecret());
        return buildQueryResult(order.getOrderNo(), order.getMerchantOrderNo(), order.getAmount(),
                order.getCurrency(), order.getPspOrderNo(), route, path, params, response, true);
    }

    @Override
    public PspOrderQueryResult queryPayoutOrder(PspOrderRequest order, PspRouteResult route) {
        requireRouteCredentials(route, "query payout order");
        Map<String, Object> params = queryParams(order.getPspOrderNo(), order.getOrderNo(), route);
        String path = "/open-api/query-payout-order";
        WorldPspHttpResponse response = post(route.getPspBaseUrl(), path, params, route.getPspAccountApiSecret());
        return buildQueryResult(order.getOrderNo(), order.getMerchantOrderNo(), order.getAmount(),
                order.getCurrency(), order.getPspOrderNo(), route, path, params, response, false);
    }

    @Override
    public PspBalanceSnap queryBalance(PspBalanceRequest request) {
        if (request == null) {
            throw new IllegalStateException("World PSP balance query request is required");
        }
        if (StringUtils.isBlank(request.getApiKey())) {
            throw new IllegalStateException("World PSP account api key is missing for balance query, pspAccountId=" + request.getPspAccountId());
        }
        if (StringUtils.isBlank(request.getApiSecret())) {
            throw new IllegalStateException("World PSP account api secret is missing for balance query, pspAccountId=" + request.getPspAccountId());
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", request.getApiKey());
        String path = "/open-api/balance";
        WorldPspHttpResponse response = postJson(request.getPspBaseUrl(), path, params, request.getApiSecret());
        return buildBalanceSnap(request, response);
    }

    private void requireRouteCredentials(PspRouteResult route, String operation) {
        if (route == null) {
            throw new IllegalStateException("World PSP route is required for " + operation);
        }
        if (StringUtils.isBlank(route.getPspAccountApiKey())) {
            throw new IllegalStateException("World PSP account api key is missing for " + operation
                    + ", pspAccountId=" + route.getPspAccountId());
        }
        if (StringUtils.isBlank(route.getPspAccountApiSecret())) {
            throw new IllegalStateException("World PSP account api secret is missing for " + operation
                    + ", pspAccountId=" + route.getPspAccountId());
        }
    }

    private Map<String, Object> payParams(PspOrderRequest order, PspRouteResult route) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", route.getPspAccountApiKey());
        params.put("merchant_order_id", order.getOrderNo());
        params.put("amount", amount(order.getAmount()));
        params.put("pay_channel", channel(route, order.getMethodCode()));
        params.put("notify_url", route.getPspCallbackUrl());
        params.put("page_return_url", order.getReturnUrl());
        mergeJson(params, order.getPayerJson());
        mergeJson(params, order.getExtraJson());
        return params;
    }

    private Map<String, Object> payoutParams(PspOrderRequest order, PspRouteResult route) {
        Map<String, Object> extra = jsonMap(order.getExtraJson());
        Map<String, Object> payee = jsonMap(order.getPayeeJson());
        String accountNo = StringUtils.defaultIfBlank(
                order.getPayeeAccountNo(),
                firstText(payee, extra, "account_no", "customer_account_no")
        );
        if (StringUtils.isBlank(accountNo)) {
            throw new IllegalStateException("World PSP payout requires payee.account_no");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", route.getPspAccountApiKey());
        params.put("merchant_order_id", order.getOrderNo());
        params.put("amount", amount(order.getAmount()));
        params.put("payout_mode", channel(route, order.getMethodCode()));
        params.put("customer_account_no", accountNo);
        params.put("notify_url", route.getPspCallbackUrl());
        putIfBlank(params, "customer_name", StringUtils.defaultIfBlank(firstText(payee, extra, "name", "customer_name"), order.getPayeeName()));
        putIfBlank(params, "customer_account_type", StringUtils.defaultIfBlank(firstText(payee, extra, "wallet_type", "customer_account_type"), order.getPayeeWalletType()));
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

    private WorldPspHttpResponse post(String baseUrl, String path, Map<String, Object> params, String secret) {
        Map<String, Object> signed = WorldPspSignUtils.withSign(params, secret);
        String url = baseUrl(baseUrl) + path;
        try {
            String body = restClient().post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(WorldPspSignUtils.formBody(signed))
                    .retrieve()
                    .body(String.class);
            return new WorldPspHttpResponse(200, parseBody(body));
        } catch (RestClientResponseException ex) {
            return new WorldPspHttpResponse(ex.getStatusCode().value(), parseBody(ex.getResponseBodyAsString()));
        }
    }

    private WorldPspHttpResponse postJson(String baseUrl, String path, Map<String, Object> params, String secret) {
        Map<String, Object> signed = WorldPspSignUtils.withSign(params, secret);
        String url = baseUrl(baseUrl) + path;
        try {
            String body = restClient().post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.toJSONString(signed, JSONWriter.Feature.WriteMapNullValue))
                    .retrieve()
                    .body(String.class);
            return new WorldPspHttpResponse(200, parseBody(body));
        } catch (RestClientResponseException ex) {
            return new WorldPspHttpResponse(ex.getStatusCode().value(), parseBody(ex.getResponseBodyAsString()));
        }
    }

    private RestClient restClient() {
        RestClient current = restClient;
        if (current == null) {
            synchronized (this) {
                current = restClient;
                if (current == null) {
                    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
                    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
                    current = RestClient.builder()
                            .requestFactory(requestFactory)
                            .build();
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

    private PspPayDispatchResult basePayResult(PspOrderRequest order, PspRouteResult route, String path, Map<String, Object> params) {
        PspPayDispatchResult result = new PspPayDispatchResult();
        result.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        result.setRequestUrl(baseUrl(route.getPspBaseUrl()) + path);
        result.setHttpMethod("POST");
        result.setRequestHeadersJson(HEADERS_JSON);
        result.setRequestBody(WorldPspSignUtils.formBody(WorldPspSignUtils.withSign(params, route.getPspAccountApiSecret())));
        result.setPspMerchantOrderNo(order.getOrderNo());
        return result;
    }

    private PspPayoutDispatchResult basePayoutResult(PspOrderRequest order, PspRouteResult route, String path, Map<String, Object> params) {
        PspPayoutDispatchResult result = new PspPayoutDispatchResult();
        result.setPspRequestNo(BizKeyUtils.genPspRequestNo());
        result.setRequestUrl(baseUrl(route.getPspBaseUrl()) + path);
        result.setHttpMethod("POST");
        result.setRequestHeadersJson(HEADERS_JSON);
        result.setRequestBody(WorldPspSignUtils.formBody(WorldPspSignUtils.withSign(params, route.getPspAccountApiSecret())));
        result.setPspMerchantOrderNo(order.getOrderNo());
        return result;
    }

    private void fillMockPayCreate(PspPayDispatchResult result, PspOrderRequest order) {
        String pspOrderNo = mockPspOrderNo(order);
        result.setSuccess(true);
        result.setHttpMethod("MOCK");
        result.setResponseStatus(200);
        result.setPspOrderNo(pspOrderNo);
        result.setPayUrl("mock://world/pay/" + order.getOrderNo());
        result.setRawStatus(PspCallbackUtils.STATUS_PROCESSING);
        result.setResponseCode("MOCK_ACCEPTED");
        result.setResponseMessage("World PSP submit skipped for internal testing");
        result.setRawResponseJson(mockCreateResponse(pspOrderNo));
    }

    private void fillMockPayoutCreate(PspPayoutDispatchResult result, PspOrderRequest order) {
        String pspOrderNo = mockPspOrderNo(order);
        result.setSuccess(true);
        result.setHttpMethod("MOCK");
        result.setResponseStatus(200);
        result.setPspOrderNo(pspOrderNo);
        result.setRawStatus(PspCallbackUtils.STATUS_PROCESSING);
        result.setResponseCode("MOCK_ACCEPTED");
        result.setResponseMessage("World PSP submit skipped for internal testing");
        result.setRawResponseJson(mockCreateResponse(pspOrderNo));
    }

    private String mockPspOrderNo(PspOrderRequest order) {
        return "MOCK" + order.getOrderNo();
    }

    private String mockCreateResponse(String pspOrderNo) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "MOCK_ACCEPTED");
        response.put("message", "World PSP submit skipped for internal testing");
        response.put("system_order_id", pspOrderNo);
        response.put("order_status", PspCallbackUtils.STATUS_PROCESSING);
        return JSON.toJSONString(response, JSONWriter.Feature.WriteMapNullValue);
    }

    private void fillPayCreate(PspPayDispatchResult result, WorldPspHttpResponse response, String secret) {
        JSONObject body = response.body();
        result.setResponseStatus(response.statusCode());
        result.setResponseCode(body.getString("code"));
        result.setResponseMessage(body.getString("message"));
        result.setRawResponseJson(body.toJSONString());
        if (response.httpSuccess() || body.getIntValue("code") != 200) {
            result.setSuccess(false);
            result.setErrorCode(StringUtils.defaultIfBlank(result.getResponseCode(), "HTTP_" + response.statusCode()));
            result.setErrorMessage(StringUtils.defaultIfBlank(result.getResponseMessage(), "World PSP HTTP status " + response.statusCode()));
            return;
        }
        JSONObject data = body.getJSONObject("data");
        if (data == null || WorldPspSignUtils.notVerify(data, secret, data.getString("sign"))) {
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

    private void fillPayoutCreate(PspPayoutDispatchResult result, WorldPspHttpResponse response, String secret) {
        JSONObject body = response.body();
        result.setResponseStatus(response.statusCode());
        result.setResponseCode(body.getString("code"));
        result.setResponseMessage(body.getString("message"));
        result.setRawResponseJson(body.toJSONString());
        if (response.httpSuccess() || body.getIntValue("code") != 200) {
            result.setSuccess(false);
            result.setSubmitResultStatus(response.httpSuccess() || isUnknownPayoutCreateResponse(body)
                    ? PspPayoutSubmitResultStatus.UNKNOWN
                    : PspPayoutSubmitResultStatus.REJECTED);
            result.setErrorCode(StringUtils.defaultIfBlank(result.getResponseCode(), "HTTP_" + response.statusCode()));
            result.setErrorMessage(StringUtils.defaultIfBlank(result.getResponseMessage(), "World PSP HTTP status " + response.statusCode()));
            return;
        }
        JSONObject data = body.getJSONObject("data");
        if (data == null || WorldPspSignUtils.notVerify(data, secret, data.getString("sign"))) {
            result.setSuccess(false);
            result.setSubmitResultStatus(PspPayoutSubmitResultStatus.UNKNOWN);
            result.setErrorCode("INVALID_SIGN");
            result.setErrorMessage("World PSP response signature invalid");
            return;
        }
        result.setSuccess(true);
        result.setSubmitResultStatus(PspPayoutSubmitResultStatus.ACCEPTED);
        result.setPspOrderNo(data.getString("system_order_id"));
        result.setResponseSign(data.getString("sign"));
        result.setRawStatus(StringUtils.defaultIfBlank(data.getString("order_status"), PspCallbackUtils.STATUS_PROCESSING));
    }

    private boolean isUnknownPayoutCreateResponse(JSONObject response) {
        String code = response.getString("code");
        return StringUtils.isBlank(code) || "0".equals(code);
    }

    private PspOrderQueryResult buildQueryResult(String systemOrderNo,
                                                 String merchantOrderNo,
                                                 BigDecimal orderAmount,
                                                 String currency,
                                                 String pspOrderNo,
                                                 PspRouteResult route,
                                                 String path,
                                                 Map<String, Object> params,
                                                 WorldPspHttpResponse response,
                                                 boolean payinOrder) {
        JSONObject body = response.body();
        PspOrderQueryResult.PspOrderQueryResultBuilder builder = PspOrderQueryResult.builder()
                .pspCode(route.getPspCode())
                .systemOrderNo(systemOrderNo)
                .merchantOrderNo(merchantOrderNo)
                .currency(currency)
                .pspRequestNo(BizKeyUtils.genPspRequestNo())
                .requestUrl(baseUrl(route.getPspBaseUrl()) + path)
                .httpMethod("POST")
                .requestHeadersJson(HEADERS_JSON)
                .requestBody(WorldPspSignUtils.formBody(WorldPspSignUtils.withSign(params, route.getPspAccountApiSecret())))
                .responseStatus(response.statusCode())
                .responseCode(body.getString("code"))
                .responseMessage(body.getString("message"))
                .rawResponseJson(body.toJSONString());

        if (response.httpSuccess() || body.getIntValue("code") != 200) {
            String errorCode = StringUtils.defaultIfBlank(body.getString("code"), "HTTP_" + response.statusCode());
            String errorMessage = StringUtils.defaultIfBlank(body.getString("message"), "World PSP HTTP status " + response.statusCode());
            return builder.success(false).errorCode(errorCode).errorMessage(errorMessage).build();
        }
        JSONObject data = body.getJSONObject("data");
        if (data == null || WorldPspSignUtils.notVerify(data, route.getPspAccountApiSecret(), data.getString("sign"))) {
            return builder.success(false).errorCode("INVALID_SIGN").errorMessage("World PSP response signature invalid").build();
        }
        String pspStatus = data.getString("order_status");
        BigDecimal amount = decimal(data.getString("amount"), orderAmount);
        return builder
                .success(true)
                .pspOrderNo(StringUtils.defaultIfBlank(data.getString("system_order_id"), pspOrderNo))
                .pspStatus(pspStatus)
                .orderStatus(payinOrder ? toPayStatus(pspStatus) : toPayoutStatus(pspStatus))
                .amount(amount)
                .responseSign(data.getString("sign"))
                .build();
    }

    private PspBalanceSnap buildBalanceSnap(PspBalanceRequest request, WorldPspHttpResponse response) {
        JSONObject body = response.body();
        PspBalanceSnap snap = new PspBalanceSnap();
        snap.setTenantId(request.getTenantId());
        snap.setPspId(request.getPspId());
        snap.setPspCode(request.getPspCode());
        snap.setPspAccountId(request.getPspAccountId());
        snap.setPspAccountNo(request.getPspAccountNo());
        snap.setUpdatedAt(Instant.now());
        snap.setRawResponseJson(body.toJSONString());
        if (response.httpSuccess() || body.getIntValue("code") != 200) {
            snap.setStatus(PspBalanceSnap.STATUS_UNKNOWN);
            snap.setErrorCode(StringUtils.defaultIfBlank(body.getString("code"), "HTTP_" + response.statusCode()));
            snap.setErrorMessage(StringUtils.defaultIfBlank(body.getString("message"), "World PSP HTTP status " + response.statusCode()));
            return snap;
        }
        JSONObject data = body.getJSONObject("data");
        if (data == null || WorldPspSignUtils.notVerify(data, request.getApiSecret(), data.getString("sign"))) {
            snap.setStatus(PspBalanceSnap.STATUS_UNKNOWN);
            snap.setErrorCode("INVALID_SIGN");
            snap.setErrorMessage("World PSP balance response signature invalid");
            return snap;
        }
        BigDecimal payoutBalance = decimal(data.getString("payout_balance"), null);
        BigDecimal balance = decimal(data.getString("balance"), null);
        if (payoutBalance == null) {
            snap.setStatus(PspBalanceSnap.STATUS_UNKNOWN);
            snap.setErrorCode("MISSING_PAYOUT_BALANCE");
            snap.setErrorMessage("World PSP balance response missing payout_balance");
            return snap;
        }
        snap.setAvailableBalance(payoutBalance);
        snap.setTotalBalance(balance);
        if (balance != null) {
            snap.setFrozenBalance(balance.subtract(payoutBalance));
        }
        snap.setStatus(PspBalanceSnap.STATUS_NORMAL);
        return snap;
    }

    private String toPayStatus(String status) {
        String value = StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PAYIN_SUCCESS", "PAY_SUCCESS", "SUCCESS", "PAID", "COMPLETED" -> PspCallbackUtils.STATUS_SUCCESS;
            case "PAYIN_FAILED", "PAY_FAILED", "PAY_FAIL", "FAILED", "CLOSED", "CANCELLED" -> PspCallbackUtils.STATUS_FAILED;
            default -> PspCallbackUtils.STATUS_PROCESSING;
        };
    }

    private String toPayoutStatus(String status) {
        String value = StringUtils.defaultString(status).trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PAYIN_SUCCESS", "PAY_SUCCESS", "SUCCESS", "COMPLETED" -> PspCallbackUtils.STATUS_SUCCESS;
            case "PAYIN_FAILED", "PAY_FAILED", "PAY_FAIL", "FAILED", "REJECTED" -> PspCallbackUtils.STATUS_FAILED;
            case "CANCELLED", "CANCELED" -> PspCallbackUtils.STATUS_CANCELLED;
            default -> PspCallbackUtils.STATUS_PROCESSING;
        };
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
            Map<String, Object> map = JSON.parseObject(json, new TypeReference<>() {
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

    private String baseUrl(String value) {
        String url = StringUtils.trimToEmpty(value);
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
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

    private record WorldPspHttpResponse(int statusCode, JSONObject body) {
        private boolean httpSuccess() {
            return statusCode < 200 || statusCode >= 300;
        }
    }
}
