package com.gk.telegram.command.handler;

import com.gk.common.model.DynMap;
import com.gk.payment.dto.PayoutOrderDTO;
import com.gk.payment.service.PayoutOrderService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * /payout 指令处理器。
 * <p>按系统单号或商户单号查询代付订单，并复用当前绑定的租户/商户范围做权限隔离。</p>
 */
@Component
@RequiredArgsConstructor
public class PayoutCommandHandler implements TgCommandHandler {
    private final PayoutOrderService payoutOrderService;

    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/payout";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "查询代付订单: /payout <单号>";
    }

    /**
     * 按平台代付单号或商户单号查询代付订单。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        String orderNo = ctx.arg(0);
        if (orderNo == null || orderNo.isBlank()) {
            return "用法: " + TgHtml.code("/payout <平台单号或商户单号>");
        }
        Long tenantId = ctx.targetTenantId();
        if (tenantId == null) {
            return "当前绑定未关联租户，无法查询代付订单。";
        }
        // 先按平台代付单号查；查不到再按商户单号查。
        PayoutOrderDTO order = queryOne(ctx, "payoutOrderNo", orderNo.trim());
        if (order == null) {
            order = queryOne(ctx, "merchantOrderNo", orderNo.trim());
        }
        if (order == null) {
            return "未查询到代付订单: " + TgHtml.code(orderNo);
        }
        return TgHtml.bold("代付订单") + "\n"
                + "平台单号: " + TgHtml.code(order.getPayoutOrderNo()) + "\n"
                + "商户单号: " + TgHtml.code(order.getMerchantOrderNo()) + "\n"
                + "金额: " + TgHtml.bold(order.getAmount() + " " + order.getCurrency()) + "\n"
                + "状态: " + TgHtml.escape(order.getStatus()) + "\n"
                + "创建时间: " + TgHtml.escape(order.getCreatedAt());
    }

    /**
     * 构造代付订单查询条件。
     * <p>个人绑定按租户查，群绑定如果有 merchantId 则进一步收窄到商户范围。</p>
     */
    private PayoutOrderDTO queryOne(TgCommandContext ctx, String field, String value) {
        DynMap params = new DynMap();
        params.put("tenantId", ctx.targetTenantId());
        if (ctx.targetMerchantId() != null) {
            params.put("merchantId", ctx.targetMerchantId());
        }
        params.put(field, value);
        List<PayoutOrderDTO> list = payoutOrderService.list(params);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}
