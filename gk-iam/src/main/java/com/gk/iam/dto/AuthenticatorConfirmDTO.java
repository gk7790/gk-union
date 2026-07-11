package com.gk.iam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthenticatorConfirmDTO {
    @NotBlank
    private String code;
}
