package com.gk.payment.notify;

import com.alibaba.fastjson2.JSON;
import com.gk.common.enums.SignTypeEnum;
import com.gk.openapi.util.ApiSignUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 商户通知签名器
 * <p>
 * OpenAPI 请求验签一致，采用 Canonical JSON
 * <pre>
 *   signText = canonicalJson(body without sign)
 *   MD5:          sign = md5(signText + apiSecret)
 *   HMAC_SHA256:  sign = hmacSha256(signText, apiSecret)
 * </pre>
 */
@Component
public class MerchantNotifySigner {

    public MerchantNotifySigned sign(String payloadJson, String apiSecret, String signType) {
        Map<String, Object> body = parse(payloadJson);
        body.remove("sign");
        String sign = SignTypeEnum.MD5.matches(signType)
                ? ApiSignUtils.createMd5Sign(body, apiSecret)
                : ApiSignUtils.createHmacSha256Sign(body, apiSecret);
        body.put("sign", sign);
        return new MerchantNotifySigned(sign, JSON.toJSONString(body));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = JSON.parseObject(json, LinkedHashMap.class);
        return map == null ? new LinkedHashMap<>() : map;
    }
}
