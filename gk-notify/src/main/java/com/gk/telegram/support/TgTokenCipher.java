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

    /**
     * 创建 AES-GCM 工具实例。
     * <p>当前密钥和盐值固定在代码中，后续如果要动态配置，可改为从 sys_params 或配置中心读取。</p>
     */
    private AesGcm aesGcm() {
        return new AesGcm("1234567890123456",  "gk-telegram");
    }

    /**
     * 加密 token 为密文(Base64Url)
     */
    public String encrypt(String token) {
        // 明文 token 只在保存时出现，落库前必须转成密文 token_cipher。
        return aesGcm().encryptBase64(token);
    }

    /**
     * 解密密文为明文 token
     */
    public String decrypt(String cipher) {
        // 调用 Telegram Bot API 前才解密，避免业务链路长期持有明文 token。
        return aesGcm().decryptStr(cipher);
    }

    /**
     * token 的 SHA-256 哈希(十六进制), 用于唯一查重, 不可逆
     */
    public String hash(String token) {
        // token_hash 用于唯一校验和查重，不用于还原 token 明文。
        return SecureUtil.sha256(token);
    }
}
