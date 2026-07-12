package com.gk.iam.service;

import com.gk.iam.dto.AuthenticatorConfirmDTO;
import com.gk.iam.dto.AuthenticatorSetupDTO;
import com.gk.iam.dto.AuthenticatorStatusDTO;

public interface UserAuthenticatorService {
    AuthenticatorStatusDTO status();

    AuthenticatorSetupDTO setup();

    void confirm(AuthenticatorConfirmDTO dto);

    void reset(Long userId);
}
