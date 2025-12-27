package com.rubberhuman.server.util;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

@Component
public class FileCryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${app.encrypt.secret}")
    private String secret;

    private SecretKeySpec getKey() {
        // 截取前16位作为 128位 AES 密钥
        byte[] keyBytes = Arrays.copyOf(secret.getBytes(), 16);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    // 流式加密：输入流 -> 加密 -> 输出流
    public void encrypt(InputStream in, OutputStream out) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getKey());

        // CipherOutputStream 会自动处理 buffer 和 padding
        try (CipherOutputStream cos = new CipherOutputStream(out, cipher)) {
            IOUtils.copy(in, cos);
        }
    }

    // 流式解密：输入流 -> 解密 -> 输出流
    public void decrypt(InputStream in, OutputStream out) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getKey());

        // CipherInputStream 读取时自动解密
        try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
            IOUtils.copy(cis, out);
        }
    }
}