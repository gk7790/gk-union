package com.gk.merchant.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.telegram.bind.TgBindPurpose;
import com.gk.telegram.bind.TgBindTicket;
import com.gk.telegram.bind.TgBindTicketService;
import com.gk.merchant.dto.MerchantDTO;
import com.gk.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商户管理")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {
    private final MerchantService merchantService;
    private final TgBindTicketService tgBindTicketService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('merchant:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<MerchantDTO> page = merchantService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('merchant:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(merchantService.get(id));
    }

    @GetMapping("dict")
    @Operation(summary = "保存")
    public R<?> dict(@RequestMap DynMap params) {
        List<MerchantDTO> list = merchantService.getDict(params);
        return R.ok(list);
    }

    @PostMapping("{id}/tg-bind-ticket")
    @Operation(summary = "生成Telegram绑定")
    @PreAuthorize("hasAuthority('merchant:info')")
    public R<?> generateTgBindTicket(@PathVariable("id") Long id,
                                     @RequestParam(defaultValue = "USER") String purpose) {
        MerchantDTO data = merchantService.get(id);
        if (data == null || data.getId() == null) {
            throw new GkException("商户不存");
        }
        TgBindTicket ticket = generateTicket(data, TgBindPurpose.parse(purpose));
        DynMap result = new DynMap();
        result.put("code", ticket.getCode());
        result.put("purpose", ticket.getPurpose());
        result.put("subjectType", ticket.getSubjectType());
        return R.ok(result);
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('merchant:save')")
    public R<?> save(@RequestBody MerchantDTO dto) {
        merchantService.save(dto);
        return R.ok(dto);
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('merchant:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody MerchantDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        merchantService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('merchant:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        merchantService.delete(ids);
        return R.ok();
    }

    private TgBindTicket generateTicket(MerchantDTO merchant, TgBindPurpose purpose) {
        Long currentMerchantId = ReqContextHolder.getMerchantId();
        Long subjectId = ReqContextHolder.getSubjectId();
        Long userId = ReqContextHolder.getUserId();
        Long tenantId = ReqContextHolder.getTenantId();
        String subjectType = ReqContextHolder.getSubjectType();
        if (merchant == null || merchant.getId() == null || !merchant.getId().equals(currentMerchantId)) {
            throw new GkException("只能为当前登录商户生成Telegram绑定");
        }
        return tgBindTicketService.generate(purpose, subjectType, tenantId != null ? tenantId : merchant.getTenantId(),
                merchant.getId(), subjectId, userId);
    }
}
