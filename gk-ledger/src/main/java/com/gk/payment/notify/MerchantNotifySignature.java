package com.gk.payment.notify;

import java.util.Map;

/**
 * 通知签名结果: 待发送的请求头(含签名)与签名值本身。
 *
 * @param signature 本次通知签名
 * @param headers   完整请求头(含 Content-Type 与平台签名头)
 */
public record MerchantNotifySignature(String signature, Map<String, String> headers) {
}
