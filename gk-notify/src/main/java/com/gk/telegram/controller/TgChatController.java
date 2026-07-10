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

import java.util.Arrays;
import java.util.List;

/**
 * Telegram 群/会话绑定后台管理接口。
 * <p>用于查看和维护群绑定、通知用途、事件订阅范围以及群解绑操作。</p>
 */
@Tag(name = "Telegram机器人-推送会话")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tg/chat")
@RequiredArgsConstructor
public class TgChatController {
    private final TgChatService tgChatService;

    /**
     * 分页查询 Telegram 会话/群绑定记录。
     */
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

    /**
     * 查询单个 Telegram 会话/群绑定详情。
     */
    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('tg:chat:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(tgChatService.get(id));
    }

    /**
     * 批量查询商户当前启用中的 Telegram 群绑定。
     */
    @GetMapping("merchant-bindings")
    @Operation(summary = "商户群绑定状态")
    @PreAuthorize("hasAuthority('tg:chat:page')")
    public R<?> merchantBindings(@RequestParam Long[] merchantIds) {
        AssertUtils.isArrayEmpty(merchantIds, "merchantIds");
        List<TgChatDTO> chats = tgChatService.listActiveMerchantChats(Arrays.asList(merchantIds));
        return R.ok(chats);
    }

    /**
     * 后台手动登记 Telegram 推送会话。
     */
    @PostMapping
    @Operation(summary = "保存", description = "登记推送目标会话/群, 可设置订阅事件event_types")
    @PreAuthorize("hasAuthority('tg:chat:save')")
    public R<?> save(@RequestBody TgChatDTO dto) {
        tgChatService.save(dto);
        return R.ok();
    }

    /**
     * 修改 Telegram 会话/群绑定配置。
     */
    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('tg:chat:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody TgChatDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        tgChatService.update(dto);
        return R.ok();
    }

    /**
     * 解绑商户当前启用中的 Telegram 群。
     */
    @DeleteMapping("merchant/{merchantId}/binding")
    @Operation(summary = "解绑商户群")
    @PreAuthorize("hasAuthority('tg:chat:delete')")
    public R<?> unbindMerchant(@PathVariable("merchantId") Long merchantId) {
        AssertUtils.isReserved(merchantId);
        tgChatService.unbindMerchantChat(merchantId);
        return R.ok();
    }

    /**
     * 给商户当前绑定的 Telegram 群发送测试通知。
     */
    @PostMapping("merchant/{merchantId}/notify-test")
    @Operation(summary = "发送商户群测试通知")
    @PreAuthorize("hasAuthority('tg:chat:update')")
    public R<?> notifyMerchant(@PathVariable("merchantId") Long merchantId) {
        AssertUtils.isReserved(merchantId);
        tgChatService.sendMerchantTestMessage(merchantId);
        return R.ok();
    }

    /**
     * 删除 Telegram 会话/群绑定配置。
     */
    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('tg:chat:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        tgChatService.delete(ids);
        return R.ok();
    }
}
