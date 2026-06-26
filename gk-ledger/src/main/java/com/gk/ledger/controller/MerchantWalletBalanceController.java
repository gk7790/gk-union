package com.gk.ledger.controller;

import com.gk.common.model.R;
import com.gk.ledger.dto.MerchantWalletBalanceDTO;
import com.gk.ledger.service.MerchantBalanceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商户钱包余额")
@RestController
@RequestMapping("/ledger/merchant-wallet/balance")
@RequiredArgsConstructor
public class MerchantWalletBalanceController {

    private final MerchantBalanceQueryService merchantBalanceQueryService;

    @GetMapping("list")
    @Operation(summary = "按币种查询当前商户钱包余额")
    public R<List<MerchantWalletBalanceDTO>> list(
            @Parameter(description = "币种，不传则返回全部币种")
            @RequestParam(required = false) String currency) {
        return R.ok(merchantBalanceQueryService.listForCurrentMerchant(currency));
    }
}
