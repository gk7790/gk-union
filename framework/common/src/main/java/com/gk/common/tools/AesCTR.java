package com.gk.common.tools;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-CTR 加解密（与 Go 版本语义一致）
 * 密文格式：
 * [16 byte IV][ciphertext]
 * Base64 URL-safe（无 padding）
 */
public class AesCTR {

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/CTR/NoPadding";
    private static final int BLOCK_SIZE = 16;

    private final byte[] key;

    public static AesCTR of() {
        return new AesCTR("b58d86abe728a680cb38ee3274a69d58");
    }

    /**
     * @param key 长度必须为 16 / 24 / 32 字节
     */
    public AesCTR(byte[] key) {
        if (key == null ||
                !(key.length == 16 || key.length == 24 || key.length == 32)) {
            throw new IllegalArgumentException("invalid key length: " +
                    (key == null ? 0 : key.length));
        }
        this.key = Arrays.copyOf(key, key.length);
    }

    /**
     * @param key 长度必须为 16 / 24 / 32 字节
     */
    public AesCTR(String key) {
        if (key == null || !(StringUtils.length(key) == 16 || StringUtils.length(key) == 24 || StringUtils.length(key) == 32)) {
            throw new IllegalArgumentException("invalid key length: " +
                    (key == null ? 0 : StringUtils.length(key)));
        }
        this.key = Arrays.copyOf(key.getBytes(), StringUtils.length(key));
    }

    /* ================= 加密 ================= */

    public String encrypt(byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            byte[] iv = new byte[BLOCK_SIZE];
            new SecureRandom().nextBytes(iv);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, AES),
                    new IvParameterSpec(iv)
            );

            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("AES-CTR 加密失败", e);
        }
    }

    public String encrypt(String plaintext) {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /* ================= 解密 ================= */

    public byte[] decrypt(String cipherTextB64) throws Exception {
        try {
            byte[] data = Base64.getUrlDecoder().decode(cipherTextB64);

            if (data.length < BLOCK_SIZE) {
                throw new IllegalArgumentException("ciphertext too short");
            }

            byte[] iv = Arrays.copyOfRange(data, 0, BLOCK_SIZE);
            byte[] ciphertext = Arrays.copyOfRange(data, BLOCK_SIZE, data.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, AES),
                    new IvParameterSpec(iv)
            );

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("AES-CTR 加密失败", e);
        }
    }

    public String decryptToString(String cipherTextB64) throws Exception {
        return new String(decrypt(cipherTextB64), StandardCharsets.UTF_8);
    }
}