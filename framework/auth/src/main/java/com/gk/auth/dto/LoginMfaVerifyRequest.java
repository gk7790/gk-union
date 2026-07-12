package com.gk.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginMfaVerifyRequest {
    @NotBlank
    private String mfaToken;

    @NotBlank
    private String mfaCode;
}
