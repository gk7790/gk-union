package com.gk.payment.notify;

/**
 * 已签名的通知报文�?
 *
 * @param sign 本次通知签名(已写�?body �?sign 字段)
 * @param body 最终发送给商户�?JSON 报文(�?sign)
 */
public record MerchantNotifySigned(String sign, String body) {
}
