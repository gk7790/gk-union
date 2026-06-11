package com.gk.payment.notify;

import com.gk.common.utils.BizKeyUtils;
import com.gk.openapi.util.ApiSignUtils;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 商户通知签名器。
 * <p>
 * 签名串约定(便于商户侧验签):
 * <pre>
 *   signText = appId + "\n" + timestamp + "\n" + nonce + "\n" + payloadJson
 *   signature = HMAC_SHA256(signText, apiSecret)   // 默认
 * </pre>
 * 商户用相同算法重算, 比对 X-Gk-Signature 即可确认通知来自平台且未被篡改。
 */
@Component
public class MerchantNotifySigner {
    public static final String HEADER_APP_ID = "X-Gk-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Gk-Timestamp";
    public static final String HEADER_NONCE = "X-Gk-Nonce";
    public static final String HEADER_SIGN_TYPE = "X-Gk-Sign-Type";
    public static final String HEADER_SIGNATURE = "X-Gk-Signature";
    public static final String HEADER_EVENT = "X-Gk-Event";
    public static final String HEADER_TASK_NO = "X-Gk-Task-No";
    public static final String HEADER_BIZ_NO = "X-Gk-Biz-No";

    private static final String SIGN_TYPE_MD5 = "MD5";

    public MerchantNotifySignature sign(MerchantNotifyTaskEntity task, String apiSecret, String payloadJson) {
        String signType = StringUtils.defaultIfBlank(task.getSignType(), "HMAC_SHA256");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = BizKeyUtils.genNonce();
        String signText = task.getAppId() + "\n" + timestamp + "\n" + nonce + "\n" + StringUtils.defaultString(payloadJson);
        String signature = SIGN_TYPE_MD5.equalsIgnoreCase(signType)
                ? ApiSignUtils.md5Hex(signText + "&key=" + apiSecret).toLowerCase(Locale.ROOT)
                : ApiSignUtils.hmacSha256Hex(signText, apiSecret).toLowerCase(Locale.ROOT);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HEADER_APP_ID, StringUtils.defaultString(task.getAppId()));
        headers.put(HEADER_TIMESTAMP, timestamp);
        headers.put(HEADER_NONCE, nonce);
        headers.put(HEADER_SIGN_TYPE, signType);
        headers.put(HEADER_SIGNATURE, signature);
        headers.put(HEADER_EVENT, StringUtils.defaultString(task.getEventType()));
        headers.put(HEADER_TASK_NO, StringUtils.defaultString(task.getTaskNo()));
        headers.put(HEADER_BIZ_NO, StringUtils.defaultString(task.getBizNo()));
        return new MerchantNotifySignature(signature, headers);
    }
}
