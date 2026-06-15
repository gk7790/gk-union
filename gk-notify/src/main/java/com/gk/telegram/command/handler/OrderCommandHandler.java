package com.gk.telegram.command.handler;

import com.gk.common.model.DynMap;
import com.gk.payment.dto.PayOrderDTO;
import com.gk.payment.service.PayOrderService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * /order 指令处理器。
 * <p>按系统订单号或商户订单号查询支付订单，并根据当前个人/群绑定范围限制可见数据。</p>
 */
@Component
@RequiredArgsConstructor
public class OrderCommandHandler implements TgCommandHandler {
    private final PayOrderService payOrderService;

    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/order";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "查询支付订单: /order <单号>";
    }

    /**
     * 按平台支付单号或商户单号查询支付订单。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        String orderNo = ctx.arg(0);
        if (orderNo == null || orderNo.isBlank()) {
            return "用法: " + TgHtml.code("/order <平台单号或商户单号>");
        }
        Long tenantId = ctx.targetTenantId();
        Long merchantId = ctx.targetMerchantId();
        if (merchantId == null) {
            return "当前绑定账号未关联商户，无法查询订单。";
        }
        if (tenantId == null) {
            return "当前绑定未关联租户，无法查询订单。";
        }

        // 先按平台单号查；查不到再按商户单号查，兼容商户侧和平台侧两种使用习惯。
        PayOrderDTO order = queryOne(ctx, "payOrderNo", orderNo.trim());
        if (order == null) {
            order = queryOne(ctx, "merchantOrderNo", orderNo.trim());
        }
        if (order == null) {
            return "未查询到支付订单: " + TgHtml.code(orderNo);
        }
        return TgHtml.bold("支付订单") + "\n"
                + "平台单号: " + TgHtml.code(order.getPayOrderNo()) + "\n"
                + "商户单号: " + TgHtml.code(order.getMerchantOrderNo()) + "\n"
                + "金额: " + TgHtml.bold(order.getAmount() + " " + order.getCurrency()) + "\n"
                + "状态: " + TgHtml.escape(order.getStatus()) + "\n"
                + "创建时间: " + TgHtml.escape(order.getCreatedAt());
    }

    /**
     * 构造订单查询条件。
     * <p>所有查询都必须同时带 tenantId + merchantId，避免租户级误查其他商户订单。</p>
     */
    private PayOrderDTO queryOne(TgCommandContext ctx, String field, String value) {
        DynMap params = new DynMap();
        params.put("tenantId", ctx.targetTenantId());
        params.put("merchantId", ctx.targetMerchantId());
        params.put(field, value);
        List<PayOrderDTO> list = payOrderService.list(params);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}
