package com.gk.telegram.command.handler;

import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.dto.MerchantBalanceDTO;
import com.gk.ledger.service.MerchantBalanceQueryService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * /balance 指令处理器。
 * <p>查询当前租户下、当前绑定商户的余额视图，不能按租户直接暴露全部账本余额。</p>
 */
@Component
@RequiredArgsConstructor
public class BalanceCommandHandler implements TgCommandHandler {
    private final MerchantBalanceQueryService merchantBalanceQueryService;

    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/balance";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "查询商户账户余额";
    }

    /**
     * 查询当前绑定商户在当前租户下的可用、冻结、待结算和总余额。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        Long tenantId = ctx.targetTenantId();
        Long merchantId = ctx.targetMerchantId();
        if (tenantId == null || merchantId == null) {
            return "当前绑定账号未关联商户，无法查询余额。";
        }

        DynMap params = new DynMap();
        params.put("tenantId", tenantId);
        params.put("merchantId", merchantId);
        params.put(Constant.PAGE, 1L);
        params.put(Constant.LIMIT, 20L);
        // 商户余额视图按 tenantId + merchantId 聚合，避免误查同租户其他商户余额。
        PageData<MerchantBalanceDTO> page = merchantBalanceQueryService.page(params);
        List<MerchantBalanceDTO> balances = page == null ? null : page.getItems();
        if (balances == null || balances.isEmpty()) {
            return "未查询到商户余额。";
        }

        StringBuilder sb = new StringBuilder(TgHtml.bold("商户账户余额")).append("\n");
        int limit = Math.min(balances.size(), 20);
        for (int i = 0; i < limit; i++) {
            MerchantBalanceDTO b = balances.get(i);
            sb.append(TgHtml.code(b.getCurrency())).append("\n")
                    .append("可用: ").append(TgHtml.bold(amount(b.getAvailableBalanceText(), b.getAvailableBalance()))).append("\n")
                    .append("冻结: ").append(TgHtml.bold(amount(b.getFrozenBalanceText(), b.getFrozenBalance()))).append("\n")
                    .append("待结算: ").append(TgHtml.bold(amount(b.getPendingSettleBalanceText(), b.getPendingSettleBalance()))).append("\n")
                    .append("总额: ").append(TgHtml.bold(amount(b.getTotalBalanceText(), b.getTotalBalance())))
                    .append("\n");
        }
        if (balances.size() > limit) {
            sb.append("... 共 ").append(balances.size()).append(" 个币种，仅展示前 ").append(limit).append(" 个");
        }
        return sb.toString().trim();
    }

    /**
     * 优先使用查询服务格式化后的展示值；字段为空时回退到原始金额。
     */
    private String amount(String text, Object value) {
        return text != null ? text : String.valueOf(value);
    }
}
