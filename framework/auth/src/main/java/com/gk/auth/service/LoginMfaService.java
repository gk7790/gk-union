package com.gk.auth.service;

import com.gk.auth.dto.MfaChallengeDTO;
import com.gk.auth.entity.SysUser;
import com.gk.auth.utils.JwtUtils;
import com.gk.common.authenticator.TotpAuthenticator;
import com.gk.common.constant.Constant;
import com.gk.common.enums.AuthTypeEnum;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginMfaService {
    public static final long CHALLENGE_EXPIRE_SECONDS = 180L;
    private static final String MFA_TYPE_TOTP = "totp";

    private final RedisUtils redisUtils;
    private final TotpAuthenticator totpAuthenticator;

    public boolean requiresMfa(SysUser user) {
        return user != null
                && AuthTypeEnum.requiresMfa(user.getAuthType())
                && StringUtils.isNotBlank(user.getAuthSecret());
    }

    public MfaChallengeDTO createChallengeResponse(SysUser user) {
        String mfaToken = UUID.randomUUID().toString().replace("-", "");
        redisUtils.set(RedisKeys.getLoginMfaChallengeKey(mfaToken), user.getId(), CHALLENGE_EXPIRE_SECONDS);

        return new MfaChallengeDTO(true, mfaToken, MFA_TYPE_TOTP, CHALLENGE_EXPIRE_SECONDS);
    }

    public Long getMfaUserId(String mfaToken) {
        if (StringUtils.isBlank(mfaToken)) {
            return null;
        }
        Object value = redisUtils.get(RedisKeys.getLoginMfaChallengeKey(mfaToken));
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNumeric(text)) {
            return Long.parseLong(text);
        }
        return null;
    }

    public boolean verify(SysUser user, String authCode) {
        return requiresMfa(user) && totpAuthenticator.verify(user.getAuthSecret(), authCode);
    }

    public Map<String, Object> buildLoginResponse(SysUser user) {
        String token = JwtUtils.generateToken(Constant.ADMIN, buildClaims(user));

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("subjectId", user.getSubjectId());
        userMap.put("username", user.getUsername());
        userMap.put("realName", user.getNickName());
        userMap.put("subjectType", user.getSubjectType());
        userMap.put("tenantId", user.getTenantId());
        userMap.put("merchantId", user.getMerchantId());
        userMap.put("deptId", user.getDeptId());
        userMap.put("roleId", user.getRoleId());
        userMap.put("roleIds", user.getRoleIdList());
        userMap.put("roles", user.getRoleList());
        userMap.put("accessToken", token);
        userMap.put("tokenType", "Bearer");
        userMap.put("expiresIn", 86400);
        return userMap;
    }

    public void deleteMfaToken(String mfaToken) {
        if (StringUtils.isNotBlank(mfaToken)) {
            redisUtils.delete(RedisKeys.getLoginMfaChallengeKey(mfaToken));
        }
    }

    private Map<String, Object> buildClaims(SysUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtUtils.USER_ID, user.getId());
        claims.put(JwtUtils.SUBJECT_ID, user.getSubjectId());
        claims.put(JwtUtils.TENANT_ID, user.getTenantId());
        claims.put(JwtUtils.MERCHANT_ID, user.getMerchantId());
        claims.put(JwtUtils.DEPT_ID, user.getDeptId());
        claims.put(JwtUtils.ROLE_ID, user.getRoleId());
        claims.put("roleIds", user.getRoleIdList());
        claims.put(JwtUtils.SUBJECT_TYPE, user.getSubjectType());
        claims.put(JwtUtils.UNAME, user.getUsername());
        claims.put(JwtUtils.SUPER_Admin, user.isSuperAdmin());
        claims.put("email", user.getEmail());
        claims.put("realName", user.getRealName());
        claims.put("roles", user.getRoleList());
        return claims;
    }
}
