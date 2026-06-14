package com.gk.telegram.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.telegram.dto.TgBotDTO;
import com.gk.telegram.service.TgBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Telegram机器人-配置管理")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tg/bot")
@RequiredArgsConstructor
public class TgBotController {
    private final TgBotService tgBotService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('tg:bot:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<TgBotDTO> page = tgBotService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('tg:bot:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(tgBotService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增机器人", description = "提交Bot Token明文, 后端加密落库并getMe回填username")
    @PreAuthorize("hasAuthority('tg:bot:save')")
    public R<?> save(@RequestBody TgBotDTO dto) {
        tgBotService.save(dto);
        return R.ok(dto);
    }

    @PutMapping("{id}")
    @Operation(summary = "修改", description = "token留空表示不修改Token")
    @PreAuthorize("hasAuthority('tg:bot:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody TgBotDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        tgBotService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('tg:bot:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        tgBotService.delete(ids);
        return R.ok();
    }

    @PostMapping("{id}/webhook")
    @Operation(summary = "设置Webhook", description = "向Telegram注册回调地址并下发secret_token")
    @PreAuthorize("hasAuthority('tg:bot:update')")
    public R<?> setupWebhook(@PathVariable("id") Long id) {
        return R.fromResult(tgBotService.setupWebhook(id));
    }

    @GetMapping("{id}/test")
    @Operation(summary = "连通测试", description = "调用getMe校验Token并回填username/botUserId")
    @PreAuthorize("hasAuthority('tg:bot:info')")
    public R<?> test(@PathVariable("id") Long id) {
        return R.fromResult(tgBotService.testConnectivity(id));
    }
}
