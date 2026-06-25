package com.gk.psp.dispatch.impl;

import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.psp.adapter.PspPayoutAdapter;
import com.gk.psp.dispatch.PspPayoutDispatchResult;
import com.gk.psp.dispatch.PspPayoutDispatchService;
import com.gk.psp.log.PspRequestLogger;
import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PSP 代付下单调度实现�? * <p>
 * 本类负责在多�?{@link PspPayoutAdapter} 中选择支持当前 PSP 的适配器，
 * 调用适配器完成上游代付提交，并把成功或失败的请求信息写入 PSP 请求日志�? */
@Service
@RequiredArgsConstructor
public class PspPayoutDispatchServiceImpl implements PspPayoutDispatchService {
    private final List<PspPayoutAdapter> adapters;
    private final PspRequestLogger pspRequestLogger;

    @Override
    public PspPayoutDispatchResult dispatch(PspOrderRequest order, PspRouteResult route) {
        // 记录开始时间，用于统计请求上游 PSP 的耗时�?        long startMs = System.currentTimeMillis();
        try {
            // 根据路由结果中的 PSP 编码选择具体适配器；每个 PSP 的接口协议由适配器负责�?            PspPayoutAdapter adapter = adapters.stream()
                    .filter(item -> item.supports(route.getPspCode()))
                    .findFirst()
                    .orElseThrow(() -> new GkException(ErrorCode.INTERNAL_SERVER_ERROR, "PSP payout adapter is not configured"));
            // 调用上游 PSP 创建代付订单，返�?PSP 订单号、原始状态、原始响应等信息�?            PspPayoutDispatchResult result = adapter.createPayoutOrder(order, route);
            // 无论 result.success 是否�?true，只要适配器正常返回，都记录一次完整的 PSP 请求日志�?            pspRequestLogger.payoutSubmitSuccess(order, route, result, System.currentTimeMillis() - startMs);
            return result;
        } catch (RuntimeException ex) {
            // 适配器不存在、网络异常、签名异常等运行时错误，记录失败日志后继续抛给上层处理解�?失败�?            pspRequestLogger.payoutSubmitFailed(order, route, ex, System.currentTimeMillis() - startMs);
            throw ex;
        }
    }
}
