package com.gk.auth.controller;

import com.gk.common.beans.CurrentUser;
import com.gk.common.dto.AuthUser;
import com.gk.common.tools.R;
import com.gk.auth.service.JpaUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@Tag(name = "部门管理")
@AllArgsConstructor
public class SysSecurityController {
    private final JpaUserDetailsService jpaUserDetailsService;
    private final CurrentUser currentUser;

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String login(Authentication authentication,
                        @RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {

        // 如果用户已经登录，重定向到首页
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/home";
        }

        // 添加错误信息
        if (error != null) {
            model.addAttribute("error", "用户名或密码错误！");
        }

        // 添加退出信息
        if (logout != null) {
            model.addAttribute("message", "您已成功退出登录！");
        }

        return "login";
    }


    /**
     * 登录页面
     */
    @GetMapping("/codes")
    public R<?> codes() {
        return R.ok(List.of( "AC_100100",
                "AC_100110",
                "AC_100120",
                "AC_100010"));
    }

    /**
     * 登录成功后的首页
     */
    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        // 获取用户信息
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("authorities", authentication.getAuthorities());
        }
        return "home";
    }

    @GetMapping("permissions")
    @Operation(summary = "权限标识")
    public R<?> permissions(){
        AuthUser user = currentUser.getAuthUser();
        Set<String> set = jpaUserDetailsService.getUserPermissions(user.getId(), user.isSAdmin());
        return R.ok(set);
    }

    /**
     * 根路径重定向
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
}
