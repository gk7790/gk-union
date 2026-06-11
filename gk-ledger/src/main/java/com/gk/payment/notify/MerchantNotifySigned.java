package com.gk.payment.notify;

/**
 * 已签名的通知报文。
 *
 * @param sign 本次通知签名(已写入 body 的 sign 字段)
 * @param body 最终发送给商户的 JSON 报文(含 sign)
 */
public record MerchantNotifySigned(String sign, String body) {
}
