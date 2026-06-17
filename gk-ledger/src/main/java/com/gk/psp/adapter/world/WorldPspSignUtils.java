package com.gk.psp.adapter.world;

import com.alibaba.fastjson2.JSONObject;
import com.gk.openapi.util.ApiSignUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class WorldPspSignUtils {
    private WorldPspSignUtils() {
    }

    public static String sign(Map<String, ?> params, String secret) {
        return ApiSignUtils.createMd5Sign(params, secret);
    }

    public static boolean verify(Map<String, ?> params, String secret, String signature) {
        return ApiSignUtils.verifyMd5Sign(params, secret, signature);
    }

    public static String canonicalText(Map<String, ?> params) {
        return ApiSignUtils.buildSortedParamString(params);
    }

    public static Map<String, Object> withSign(Map<String, Object> params, String secret) {
        Map<String, Object> signed = new LinkedHashMap<>(params);
        signed.put("sign", sign(signed, secret));
        return signed;
    }

    public static String formBody(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return params.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(formValue(entry.getValue())))
                .map(entry -> encode(entry.getKey().trim()) + "=" + encode(formValue(entry.getValue())))
                .collect(Collectors.joining("&"));
    }

    private static String formValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence text) {
            return text.toString().trim();
        }
        return String.valueOf(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        Map<String, Object> params = new HashMap<>();
        params.put("app_id", "G87QQS3WPWQ7EKTXE79FG57N3CX");

//        params.put("merchant_order_id", "546dcec158c24a4cb439360e4db86415");
//        params.put("amount", "100.00");
//        params.put("pay_channel", "PHI_MAYA");
//        params.put("notify_url", "http://mqmq.vip.cpolar.cn/psp/callback/WP001/pay");
//        params.put("page_return_url", "http://mqmq.vip.cpolar.cn/sys/page");

//        params.put("merchant_order_id", "W4894651654654121");
//        params.put("amount", "100.00");
//        params.put("payout_mode", "PHI_MAYA");
//        params.put("customer_account_type", "");
//        params.put("customer_account_no", "01234567890");
//        params.put("notify_url", "https://merchant.example.com/return");


        params.put("merchant_order_id", "S4894651654654121");
        params.put("amount", "100.00");
        params.put("payout_mode", "PHI_MAYA");
        params.put("customer_account_type", "");
        params.put("customer_account_no", "01234567890");
        params.put("notify_url", "https://merchant.example.com/return");

        String secret = "31Lskdca7sflDiBncR1Ljgzo8Tij11o8XlI301";

        System.out.println(JSONObject.toJSONString(withSign(params, secret)));
    }
}
