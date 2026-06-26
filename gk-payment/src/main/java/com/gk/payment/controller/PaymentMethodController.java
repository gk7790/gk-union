package com.gk.payment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.payment.dto.PaymentMethodDTO;
import com.gk.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "支付方式")
@RestController
@RequestMapping("/payment/method")
@RequiredArgsConstructor
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('payment:method:page')")
    public R<PageData<PaymentMethodDTO>> page(@RequestMap DynMap params) {
        return R.ok(paymentMethodService.page(params));
    }

    @GetMapping("options")
    @Operation(summary = "支付方式缓存选项", description = "返回可用于管理后台前端缓存的支付方式配置全量列表，前端按方向、国地区、币种自行通配过滤")
    public R<List<PaymentMethodDTO>> options(@RequestMap DynMap params) {
        return R.ok(paymentMethodService.getOptions(params));
    }

    @GetMapping("dict")
    @Operation(summary = "支付方式字典", description = "按方向、国地区、币种、类型查询可用支付方")
    public R<List<PaymentMethodDTO>> dict(@RequestMap DynMap params) {
        return R.ok(paymentMethodService.getDict(params));
    }

    @GetMapping("label-dict")
    @Operation(summary = "支付方式下拉字典")
    public R<List<LabelDTO>> labelDict(@RequestMap DynMap params) {
        return R.ok(paymentMethodService.getLabelDict(params));
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('payment:method:info')")
    public R<PaymentMethodDTO> get(@PathVariable("id") Long id) {
        return R.ok(paymentMethodService.get(id));
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('payment:method:save')")
    public R<Void> save(@RequestBody PaymentMethodDTO dto) {
        paymentMethodService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('payment:method:update')")
    public R<Void> update(@PathVariable("id") Long id, @RequestBody PaymentMethodDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        paymentMethodService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('payment:method:delete')")
    public R<Void> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        paymentMethodService.delete(ids);
        return R.ok();
    }
}
