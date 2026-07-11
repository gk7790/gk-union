package com.gk.psp.dispatch;

import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;

/**
 * PSP 代收下单调度服务 * <p>
 * OpenAPI 创建代收订单并完成路由后，通过本接口把订单提交到具PSP 适配器 */
public interface PspPayDispatchService {
    /**
     * 提交代收订单到路由选中PSP     *
     * @param order 平台代收订单
     * @param route PSP 路由结果，包PSP 编码、账户、方法和密钥等信     * @return PSP 下单结果，包PSP 单号、支付链接、原始响应等
     */
    PspPayDispatchResult dispatch(PspOrderRequest order, PspRouteResult route);
}
