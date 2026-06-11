package com.gk.telegram.command.handler;

import com.gk.common.model.DynMap;
import com.gk.payment.dto.PayOrderDTO;
import com.gk.payment.service.PayOrderService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * /order 查询订单: /order &lt;平台单号或商户单号&gt;
 */
@Component
@RequiredArgsConstructor
public class OrderCommandHandler implements TgCommandHandler {
    private final PayOrderService payOrderService;

    @Override
    public String command() {
        return "/order";
    }

    @Override
    public String description() {
        return "查询订单: /order <单号>";
    }

    @Override
    public String handle(TgCommandContext ctx) {
        String orderNo = ctx.arg(0);
        if (orderNo == null || orderNo.isBlank()) {
            return "用法: /order <平台单号或商户单号>";
        }
        Long tenantId = ctx.getAccount().getTenantId();
        if (tenantId == null) {
            return "当前绑定账号未关联租户, 无法查询订单。";
        }

        // 先按平台单号查, 查不到再按商户单号查, 始终带租户隔离
        PayOrderDTO order = queryOne(tenantId, "payOrderNo", orderNo.trim());
        if (order == null) {
            order = queryOne(tenantId, "merchantOrderNo", orderNo.trim());
        }
        if (order == null) {
            return "未查询到订单: " + orderNo;
        }
        return "订单详情:\n"
                + "平台单号: " + order.getPayOrderNo() + "\n"
                + "商户单号: " + order.getMerchantOrderNo() + "\n"
                + "金额: " + order.getAmount() + " " + order.getCurrency() + "\n"
                + "状态: " + order.getStatus() + "\n"
                + "创建时间: " + order.getCreatedAt();
    }

    private PayOrderDTO queryOne(Long tenantId, String field, String value) {
        DynMap params = new DynMap();
        params.put("tenantId", tenantId);
        params.put(field, value);
        List<PayOrderDTO> list = payOrderService.list(params);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}
