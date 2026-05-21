package com.gk.common.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class AesGcm extends SymmetricCrypto {

    private static final long serialVersionUID = 1L;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BIT_LENGTH = 128;

    private final String salt;

    public AesGcm(byte[] key, String salt) {
        super("AES/GCM/NoPadding", KeyUtil.generateKey(SymmetricAlgorithm.AES.getValue(), key));
        this.salt = salt;
    }

    public AesGcm(String key, String salt) {
        this(key.getBytes(StandardCharsets.UTF_8), salt);
    }

    public AesGcm(String salt) {
        this("1234567890123456".getBytes(StandardCharsets.UTF_8), salt);
    }

    @Override
    public String encryptBase64(String data) {
        byte[] encrypted = encrypt(data);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
    }

    @Override
    public String decryptStr(String base64Data) {
        byte[] decode = java.util.Base64.getUrlDecoder().decode(base64Data);
        return decryptStr(decode);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        try {
            byte[] iv = generateSaltedIv(salt);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, this.getSecretKey(), spec);
            byte[] encrypted = cipher.doFinal(data);
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM 加密失败", e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) {
        try {
            byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
            byte[] actualCipher = Arrays.copyOfRange(data, IV_LENGTH, data.length);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, this.getSecretKey(), spec);
            return cipher.doFinal(actualCipher);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM 解密失败", e);
        }
    }

    // 盐值混淆 IV
    private byte[] generateSaltedIv(String salt) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        if (StrUtil.isNotBlank(salt)) {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] mixed = mac.doFinal(iv);
            return Arrays.copyOf(mixed, IV_LENGTH);
        }
        return iv;
    }
}