package com.gk.payment.notify;

import com.alibaba.fastjson2.JSON;
import com.gk.common.enums.SignTypeEnum;
import com.gk.openapi.util.ApiSignUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 商户通知签名器。
 * <p>
 * 采用传统支付"排序参数签名"方式, 签名直接写入报文体的 {@code sign} 字段(不放请求头):
 * <pre>
 *   signText = key1=value1&key2=value2... (按 key 升序, 跳过空值与 sign 本身)
 *   MD5:          sign = md5(signText + "&key=" + apiSecret)
 *   HMAC_SHA256:  sign = hmacSha256(signText, apiSecret)
 * </pre>
 * 商户用同样算法对收到的字段(去掉 sign)重算并比对即可验签。
 */
@Component
public class MerchantNotifySigner {

    /**
     * 对通知报文签名, 并把 sign 注入 body。
     *
     * @param payloadJson 不含 sign 的业务报文
     * @param apiSecret   商户密钥
     * @param signType    MD5 / HMAC_SHA256
     */
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
