package com.gk.infra.config.controller;

import com.gk.common.model.R;
import com.gk.infra.config.dto.RuntimeParamUpdateDTO;
import com.gk.infra.config.service.RuntimeParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sys/runtime-params")
@Tag(name = "运行参数")
@RequiredArgsConstructor
public class RuntimeParamController {

    private final RuntimeParamService runtimeParamService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "运行参数列表")
    public R<?> list() {
        return R.ok(runtimeParamService.list());
    }

    @PutMapping("{paramCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "修改运行参数")
    public R<?> update(@PathVariable String paramCode, @RequestBody RuntimeParamUpdateDTO dto) {
        runtimeParamService.update(paramCode, dto);
        return R.ok();
    }
}
