package com.gk.psp.enums;

public enum PspPayoutSubmitStatus {
    /**
     * PSP 已受理代付提交请求。
     */
    ACCEPTED,

    /**
     * PSP 明确拒绝订单本身，平台订单可以进入失败终态。
     */
    REJECTED,

    /**
     * PSP 提交结果未知，查单或回调确认前不能切换路由。
     */
    UNKNOWN,

    /**
     * 当前路由资源不可用，例如 PSP 余额不足或账号停用。
     * 平台订单本身未被拒绝，可以尝试切换其它路由。
     */
    ROUTE_UNAVAILABLE
}
