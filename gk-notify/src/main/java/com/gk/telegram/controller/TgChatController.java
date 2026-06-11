package com.gk.telegram.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.telegram.dto.TgChatDTO;
import com.gk.telegram.service.TgChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Telegram机器人-推送会话")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tg/chat")
@RequiredArgsConstructor
public class TgChatController {
    private final TgChatService tgChatService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('tg:chat:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<TgChatDTO> page = tgChatService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('tg:chat:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(tgChatService.get(id));
    }

    @PostMapping
    @Operation(summary = "保存", description = "登记推送目标会话/群, 可设置订阅事件event_types")
    @PreAuthorize("hasAuthority('tg:chat:save')")
    public R<?> save(@RequestBody TgChatDTO dto) {
        tgChatService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('tg:chat:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody TgChatDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        tgChatService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('tg:chat:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        tgChatService.delete(ids);
        return R.ok();
    }
}
