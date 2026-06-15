package com.gk.telegram.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.telegram.dto.TgAccountDTO;
import com.gk.telegram.service.TgAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Telegram 个人账号绑定后台管理接口。
 * <p>用于查看 Telegram 用户与系统租户/商户账号的绑定关系，并支持后台解绑。</p>
 */
@Tag(name = "Telegram机器人-账号绑定")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tg/account")
@RequiredArgsConstructor
public class TgAccountController {
    private final TgAccountService tgAccountService;

    /**
     * 分页查询 Telegram 用户绑定记录，供后台管理和排查绑定关系使用。
     */
    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('tg:account:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<TgAccountDTO> page = tgAccountService.page(params);
        return R.ok(page);
    }

    /**
     * 查询单条 Telegram 用户绑定详情。
     */
    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('tg:account:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(tgAccountService.get(id));
    }

    /**
     * 后台手动解绑 Telegram 用户绑定。
     */
    @PostMapping("{id}/unbind")
    @Operation(summary = "解绑", description = "将绑定置为失效(status=0)")
    @PreAuthorize("hasAuthority('tg:account:update')")
    public R<?> unbind(@PathVariable("id") Long id) {
        tgAccountService.unbind(id);
        return R.ok();
    }
}
