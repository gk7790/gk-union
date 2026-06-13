package com.gk.psp.dispatch.impl;

import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.psp.adapter.PspPayAdapter;
import com.gk.psp.dispatch.PspPayDispatchResult;
import com.gk.psp.dispatch.PspPayDispatchService;
import com.gk.psp.log.PspRequestLogger;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PSP 代收下单调度实现。
 * <p>
 * 本类负责在多个 {@link PspPayAdapter} 中选择支持当前 PSP 的适配器，
 * 调用适配器完成上游代收下单，并把成功或失败的请求信息写入 PSP 请求日志。
 */
@Service
@RequiredArgsConstructor
public class PspPayDispatchServiceImpl implements PspPayDispatchService {
    private final List<PspPayAdapter> adapters;
    private final PspRequestLogger pspRequestLogger;

    @Override
    public PspPayDispatchResult dispatch(PayOrderEntity order, PspRouteResult route) {
        // 记录开始时间，用于统计请求上游 PSP 的耗时。
        long startMs = System.currentTimeMillis();
        try {
            // 根据路由结果中的 PSP 编码选择具体适配器；每个 PSP 的接口协议由适配器负责。
            PspPayAdapter adapter = adapters.stream()
                    .filter(item -> item.supports(route.getPspCode()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP adapter is not configured"));
            // 调用上游 PSP 创建代收订单，返回支付链接、PSP 订单号、原始响应等信息。
            PspPayDispatchResult result = adapter.createPayOrder(order, route);
            // 无论 result.success 是否为 true，只要适配器正常返回，都记录一次完整的 PSP 请求日志。
            pspRequestLogger.paySubmitSuccess(order, route, result, System.currentTimeMillis() - startMs);
            return result;
        } catch (RuntimeException ex) {
            // 适配器不存在、网络异常、签名异常等运行时错误，记录失败日志后继续抛给上层处理订单失败。
            pspRequestLogger.paySubmitFailed(order, route, ex, System.currentTimeMillis() - startMs);
            throw ex;
        }
    }
}
