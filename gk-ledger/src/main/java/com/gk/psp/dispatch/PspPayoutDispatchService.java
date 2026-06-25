package com.gk.psp.dispatch;

import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;

/**
 * PSP 代付下单调度服务�? * <p>
 * 平台冻结商户余额并完�?PSP 路由后，通过本接口把代付订单提交到具�?PSP 适配器�? */
public interface PspPayoutDispatchService {
    /**
     * 提交代付订单到路由选中�?PSP�?     *
     * @param order 平台代付订单
     * @param route PSP 路由结果，包�?PSP 编码、账户、方法和密钥等信�?     * @return PSP 代付提交结果，包�?PSP 单号、原始状态、原始响应等
     */
    PspPayoutDispatchResult dispatch(PspOrderRequest order, PspRouteResult route);
}
