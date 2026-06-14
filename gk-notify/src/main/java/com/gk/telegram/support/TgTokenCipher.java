package com.gk.telegram.support;

import cn.hutool.crypto.SecureUtil;
import com.gk.common.tools.AesGcm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bot Token 加解密与哈希: 密文落库 token_cipher, 哈希落库 token_hash(查重)
 */
@Component
@RequiredArgsConstructor
public class TgTokenCipher {

    private AesGcm aesGcm() {
        return new AesGcm("1234567890123456",  "gk-telegram");
    }

    /**
     * 加密 token 为密文(Base64Url)
     */
    public String encrypt(String token) {
        return aesGcm().encryptBase64(token);
    }

    /**
     * 解密密文为明文 token
     */
    public String decrypt(String cipher) {
        return aesGcm().decryptStr(cipher);
    }

    /**
     * token 的 SHA-256 哈希(十六进制), 用于唯一查重, 不可逆
     */
    public String hash(String token) {
        return SecureUtil.sha256(token);
    }
}
