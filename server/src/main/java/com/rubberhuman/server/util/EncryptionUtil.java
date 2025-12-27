package com.rubberhuman.server.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {

    // 从配置文件读取加密密钥
    @Value("${app.encrypt.secret}")
    private String rootKeyString;

    private static final String ALGORITHM = "AES";
    // AES/GCM/NoPadding 自带完整性校验
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    // GCM 推荐 IV 长度为 12 字节
    private static final int IV_LENGTH_BYTE = 12;
    // GCM 认证标签长度 (128位)
    private static final int TAG_LENGTH_BIT = 128;

    /**
     * 加密
     * @param plainText 明文密码
     * @return Base64编码的密文 (格式: IV + CipherText)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        try {
            // 1. 准备密钥
            SecretKey secretKey = getSecretKey(rootKeyString);

            // 2. 生成随机 IV (初始化向量)
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            // 3. 初始化 Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 4. 执行加密
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 5. 拼接 IV + 密文 (解密时需要 IV)
            byte[] encrypted = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);

            // 6. 转为 Base64 字符串存储
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密
     * @param encryptedText Base64编码的密文
     * @return 明文密码
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }
        try {
            // 1. Base64 解码
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // 2. 提取 IV (前12字节)
            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH_BYTE);

            // 3. 提取真正的密文 (剩余部分)
            int cipherTextLength = decoded.length - IV_LENGTH_BYTE;
            byte[] cipherText = new byte[cipherTextLength];
            System.arraycopy(decoded, IV_LENGTH_BYTE, cipherText, 0, cipherTextLength);

            // 4. 准备密钥
            SecretKey secretKey = getSecretKey(rootKeyString);

            // 5. 初始化 Cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 6. 执行解密
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("解密失败: 密钥错误或数据被篡改", e);
        }
    }

    /**
     * 辅助方法：将配置文件中的字符串转换为符合 AES 要求的 256位 密钥
     * 原理：对任意长度字符串做 SHA-256 哈希，得到固定的 32字节(256位) 数组
     */
    private SecretKey getSecretKey(String myKey) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(myKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, ALGORITHM);
    }
}
