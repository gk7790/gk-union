package com.gk.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MfaChallengeDTO {
    private Boolean mfaRequired;
    private String mfaToken;
    private String mfaType;
    private Long expiresIn;
}
