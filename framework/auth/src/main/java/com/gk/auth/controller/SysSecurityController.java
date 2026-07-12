package com.gk.auth.controller;

import com.gk.auth.service.JpaUserDetailsService;
import com.gk.auth.utils.SecurityUtils;
import com.gk.common.dto.AuthUser;
import com.gk.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Tag(name = "认证", description = "登录、退出与当前用户权限")
@AllArgsConstructor
public class SysSecurityController {

    private final JpaUserDetailsService userDetailsService;
    private final SecurityUtils securityUtils;

    @PostMapping("user/logout")
    @Operation(summary = "退出登录", description = "清理当前用户所有登录鉴权相关 Redis 缓存，并清空 Security 上下文")
    public R<?> logout() {
        AuthUser user = securityUtils.getAuthUser();
        if (user != null) {
            userDetailsService.evictLoginCache(user.getId(), user.getSubjectId(), user.getDeptId());
        }
        SecurityContextHolder.clearContext();
        return R.ok();
    }

    /**
     * 登录页面
     */
    @GetMapping("/user/codes")
    public R<?> codes() {
        AuthUser user = securityUtils.getAuthUser();
        Set<String> result = Stream.concat(
                Optional.ofNullable(user.getRoleList()).orElse(Collections.emptyList()).stream(),
                Optional.ofNullable(user.getAuthList()).orElse(Collections.emptyList()).stream()
        ).collect(Collectors.toSet());
        return R.ok(result);
    }
}
