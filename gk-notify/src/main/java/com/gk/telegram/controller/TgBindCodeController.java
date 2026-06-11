package com.gk.telegram.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.telegram.dto.TgBindCodeDTO;
import com.gk.telegram.entity.TgBindCodeEntity;
import com.gk.telegram.service.TgBindCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Telegram机器人-绑定码")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tg/bind-code")
@RequiredArgsConstructor
public class TgBindCodeController {
    private final TgBindCodeService tgBindCodeService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('tg:bind-code:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<TgBindCodeDTO> page = tgBindCodeService.page(params);
        return R.ok(page);
    }

    @PostMapping("generate")
    @Operation(summary = "生成绑定码", description = "为当前登录用户生成一次性绑定码, 在Telegram中发送 /bind <绑定码> 完成绑定")
    @PreAuthorize("hasAuthority('tg:bind-code:generate')")
    public R<?> generate() {
        Long tenantId = ReqContextHolder.getTenantId();
        Long userId = ReqContextHolder.getUserId();
        TgBindCodeEntity entity = tgBindCodeService.generate(tenantId, userId, null);
        TgBindCodeDTO dto = new TgBindCodeDTO();
        dto.setCode(entity.getCode());
        dto.setExpireAt(entity.getExpireAt());
        return R.ok(dto);
    }
}
