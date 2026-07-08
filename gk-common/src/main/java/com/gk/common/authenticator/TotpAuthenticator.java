package com.gk.common.authenticator;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class TotpAuthenticator {
    private static final String DEFAULT_ISSUER = "GK-Union";
    private static final String CODE_PATTERN = "\\d{6}";

    private final GoogleAuthenticator googleAuthenticator;

    public TotpAuthenticator() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(6)
                .setTimeStepSizeInMillis(30_000)
                .setWindowSize(1)
                .build();
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }

    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    public boolean verify(String secret, String code) {
        if (StringUtils.isBlank(secret) || StringUtils.isBlank(code)) {
            return false;
        }
        if (!code.matches(CODE_PATTERN)) {
            return false;
        }
        return googleAuthenticator.authorize(secret, Integer.parseInt(code));
    }

    public boolean verify(String secret, int code) {
        return verify(secret, String.valueOf(code));
    }

    public String buildOtpAuthUrl(String account, String secret) {
        return buildOtpAuthUrl(DEFAULT_ISSUER, account, secret);
    }

    public String buildOtpAuthUrl(String issuer, String account, String secret) {
        String normalizedIssuer = StringUtils.defaultIfBlank(issuer, DEFAULT_ISSUER);
        String encodedIssuer = encode(normalizedIssuer);
        String encodedAccount = encode(account);
        String encodedSecret = encode(secret);

        return "otpauth://totp/" + encodedIssuer + ":" + encodedAccount
                + "?secret=" + encodedSecret
                + "&issuer=" + encodedIssuer
                + "&digits=6"
                + "&period=30";
    }

    private String encode(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }
}
