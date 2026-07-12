package com.gk.auth.controller;

import com.gk.auth.dto.LoginMfaVerifyRequest;
import com.gk.auth.entity.SysUser;
import com.gk.auth.service.JpaUserDetailsService;
import com.gk.auth.service.LoginMfaService;
import com.gk.common.constant.Constant;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "Login MFA")
public class LoginMfaController {
    private final LoginMfaService loginMfaService;
    private final JpaUserDetailsService userDetailsService;

    @PostMapping("/verify")
    @Operation(summary = "Verify login MFA code")
    public R<?> verify(@Valid @RequestBody LoginMfaVerifyRequest request) {
        Long userId = loginMfaService.getMfaUserId(request.getMfaToken());
        if (userId == null) {
            return R.error(ErrorCode.TOKEN_INVALID);
        }

        SysUser user = userDetailsService.getUserByUserId(Constant.ADMIN, userId);
        if (!loginMfaService.verify(user, request.getMfaCode())) {
            return R.error(ErrorCode.AUTH_CODE_ERROR);
        }

        loginMfaService.deleteMfaToken(request.getMfaToken());
        return R.ok(loginMfaService.buildLoginResponse(user));
    }
}
