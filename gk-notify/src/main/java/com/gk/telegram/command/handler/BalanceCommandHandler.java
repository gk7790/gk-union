package com.gk.telegram.command.handler;

import com.gk.common.model.DynMap;
import com.gk.ledger.dto.LedgerBalanceDTO;
import com.gk.ledger.service.LedgerBalanceService;
import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * /balance 查询当前租户账户余额
 */
@Component
@RequiredArgsConstructor
public class BalanceCommandHandler implements TgCommandHandler {
    private final LedgerBalanceService ledgerBalanceService;

    @Override
    public String command() {
        return "/balance";
    }

    @Override
    public String description() {
        return "查询账户余额";
    }

    @Override
    public String handle(TgCommandContext ctx) {
        Long tenantId = ctx.getAccount().getTenantId();
        if (tenantId == null) {
            return "当前绑定账号未关联租户, 无法查询余额。";
        }
        DynMap params = new DynMap();
        params.put("tenantId", tenantId);
        List<LedgerBalanceDTO> balances = ledgerBalanceService.list(params);
        if (balances == null || balances.isEmpty()) {
            return "未查询到余额账户。";
        }
        StringBuilder sb = new StringBuilder("账户余额:\n");
        int limit = Math.min(balances.size(), 20);
        for (int i = 0; i < limit; i++) {
            LedgerBalanceDTO b = balances.get(i);
            sb.append(b.getAccountNo())
                    .append(" [").append(b.getCurrency()).append("] ")
                    .append(b.getBalance())
                    .append("\n");
        }
        if (balances.size() > limit) {
            sb.append("... 共 ").append(balances.size()).append(" 个账户, 仅展示前 ").append(limit).append(" 个");
        }
        return sb.toString().trim();
    }
}
