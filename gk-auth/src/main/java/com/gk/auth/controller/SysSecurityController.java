package com.gk.auth.controller;

import com.gk.auth.utils.SecurityUtils;
import com.gk.common.constant.Constant;
import com.gk.common.dto.AuthUser;
import com.gk.common.model.R;
import com.gk.auth.service.JpaUserDetailsService;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping()
@Tag(name = "部门管理")
@AllArgsConstructor
public class SysSecurityController {
    private final JpaUserDetailsService jpaUserDetailsService;
    private final SecurityUtils securityUtils;
    private final RedisUtils redisUtils;

    /**
     * 登录页面
     */
    @PostMapping("user/logout")
    public R<?> logout() {
        AuthUser user = securityUtils.getAuthUser();
        List<String> keys = List.of(
                RedisKeys.getSysLonginKey(Constant.ADMIN, user.getId() + ""),
                RedisKeys.getDeptIdsKey(user.getDeptId())
        );
        redisUtils.delete(keys);
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

    /**
     * 根路径重定向
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
}
