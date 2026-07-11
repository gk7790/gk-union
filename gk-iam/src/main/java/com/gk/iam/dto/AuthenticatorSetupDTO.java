package com.gk.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatorSetupDTO {
    private String secret;
    private String otpauthUrl;
    private Long expiresIn;
}
