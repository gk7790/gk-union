package com.gk.iam.service.impl;

import com.gk.common.authenticator.TotpAuthenticator;
import com.gk.common.beans.CurrentUser;
import com.gk.common.enums.AuthTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.iam.dto.AuthenticatorConfirmDTO;
import com.gk.iam.dto.AuthenticatorSetupDTO;
import com.gk.iam.dto.AuthenticatorStatusDTO;
import com.gk.iam.entity.SysUserEntity;
import com.gk.iam.service.SysUserService;
import com.gk.iam.service.UserAuthenticatorService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAuthenticatorServiceImpl implements UserAuthenticatorService {
    private static final long BIND_EXPIRE_SECONDS = 600L;
    private static final String MFA_TYPE_TOTP = "totp";

    private final CurrentUser currentUser;
    private final RedisUtils redisUtils;
    private final TotpAuthenticator totpAuthenticator;
    private final SysUserService sysUserService;

    @Override
    public AuthenticatorStatusDTO status() {
        SysUserEntity user = currentUserEntity();
        boolean bound = isBound(user);
        return new AuthenticatorStatusDTO(bound, user.getAuthType(), bound ? MFA_TYPE_TOTP : null);
    }

    @Override
    public AuthenticatorSetupDTO setup() {
        SysUserEntity user = currentUserEntity();
        assertNotBound(user);

        String secret = totpAuthenticator.generateSecret();
        redisUtils.set(RedisKeys.getAuthenticatorBindKey(user.getId()), secret, BIND_EXPIRE_SECONDS);

        String account = StringUtils.defaultIfBlank(user.getUsername(), String.valueOf(user.getId()));
        String otpauthUrl = totpAuthenticator.buildOtpAuthUrl(account, secret);
        return new AuthenticatorSetupDTO(secret, otpauthUrl, BIND_EXPIRE_SECONDS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(AuthenticatorConfirmDTO dto) {
        SysUserEntity user = currentUserEntity();
        assertNotBound(user);

        String secret = redisUtils.get(RedisKeys.getAuthenticatorBindKey(user.getId()), String.class);
        if (StringUtils.isBlank(secret) || dto == null || !totpAuthenticator.verify(secret, dto.getCode())) {
            throw new GkException(ErrorCode.AUTH_CODE_ERROR);
        }

        sysUserService.updateAuthenticator(user.getId(), AuthTypeEnum.TOTP.code(), secret);
        redisUtils.delete(RedisKeys.getAuthenticatorBindKey(user.getId()));
        evictLoginCache(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reset(Long userId) {
        AssertUtils.isNull(userId, "id");
        if (userId.equals(currentUser.getUserId())) {
            throw new GkException(ErrorCode.FAILURE);
        }

        SysUserEntity user = sysUserService.selectById(userId);
        if (user == null) {
            throw new GkException(ErrorCode.ACCOUNT_NOT_EXIST);
        }

        sysUserService.updateAuthenticator(userId, AuthTypeEnum.NONE.code(), null);
        redisUtils.delete(RedisKeys.getAuthenticatorBindKey(userId));
        evictLoginCache(user);
    }

    private SysUserEntity currentUserEntity() {
        Long userId = currentUser.getUserId();
        if (userId == null) {
            throw new GkException(ErrorCode.UNAUTHORIZED);
        }
        SysUserEntity user = sysUserService.selectById(userId);
        if (user == null) {
            throw new GkException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        return user;
    }

    private void assertNotBound(SysUserEntity user) {
        if (isBound(user)) {
            throw new GkException(ErrorCode.FAILURE);
        }
    }

    private boolean isBound(SysUserEntity user) {
        return user != null
                && AuthTypeEnum.requiresMfa(user.getAuthType())
                && StringUtils.isNotBlank(user.getAuthSecret());
    }

    private void evictLoginCache(SysUserEntity user) {
        if (user == null || user.getId() == null) {
            return;
        }
        redisUtils.delete(RedisKeys.getSysLonginKey(com.gk.common.constant.Constant.ADMIN, user.getId().toString()));
        redisUtils.delete(RedisKeys.getSecurityUserKey(user.getId()));
        redisUtils.delete(RedisKeys.getUserMenuNavKey(user.getId()));
        redisUtils.delete(RedisKeys.getUserPermissionsKey(user.getId()));
    }

    public static void main(String[] args) {

    }
}
