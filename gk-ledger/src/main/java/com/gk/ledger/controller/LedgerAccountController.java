package com.gk.ledger.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.ledger.dto.LedgerAccountDTO;
import com.gk.ledger.service.LedgerAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "账务账户")
@RestController
@RequestMapping("/ledger/account")
@RequiredArgsConstructor
public class LedgerAccountController {
    private final LedgerAccountService ledgerAccountService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开�?, in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录�?, in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选�?asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('ledger:account:page')")
    public R<PageData<LedgerAccountDTO>> page(@RequestMap DynMap params) {
        PageData<LedgerAccountDTO> page = ledgerAccountService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('ledger:account:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(ledgerAccountService.get(id));
    }
}
