package com.gk.iam.controller;

import com.gk.common.model.R;
import com.gk.iam.dto.AuthenticatorConfirmDTO;
import com.gk.iam.service.UserAuthenticatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Authenticator")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class UserAuthenticatorController {
    private final UserAuthenticatorService userAuthenticatorService;

    @GetMapping("/authenticator/status")
    @Operation(summary = "Current user authenticator status")
    public R<?> status() {
        return R.ok(userAuthenticatorService.status());
    }

    @PostMapping("/authenticator/setup")
    @Operation(summary = "Create current user authenticator binding challenge")
    public R<?> setup() {
        return R.ok(userAuthenticatorService.setup());
    }

    @PostMapping("/authenticator/confirm")
    @Operation(summary = "Confirm current user authenticator binding")
    public R<?> confirm(@Valid @RequestBody AuthenticatorConfirmDTO dto) {
        userAuthenticatorService.confirm(dto);
        return R.ok();
    }

    @PostMapping("/{id}/authenticator/reset")
    @Operation(summary = "Admin reset user authenticator")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public R<?> reset(@PathVariable("id") Long id) {
        userAuthenticatorService.reset(id);
        return R.ok();
    }
}
