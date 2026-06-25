package com.gk.ledger.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.ledger.dto.LedgerJournalDTO;
import com.gk.ledger.service.LedgerJournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "账务凭证")
@RestController
@RequestMapping("/ledger/journal")
@RequiredArgsConstructor
public class LedgerJournalController {
    private final LedgerJournalService ledgerJournalService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开�?, in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录�?, in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选�?asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('ledger:journal:page')")
    public R<PageData<LedgerJournalDTO>> page(@RequestMap DynMap params) {
        PageData<LedgerJournalDTO> page = ledgerJournalService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('ledger:journal:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(ledgerJournalService.get(id));
    }
}
