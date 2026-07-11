package com.gk.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatorStatusDTO {
    private Boolean bound;
    private Integer authType;
    private String mfaType;
}
