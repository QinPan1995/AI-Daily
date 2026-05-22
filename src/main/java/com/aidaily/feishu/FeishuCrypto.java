package com.aidaily.feishu;

import com.aidaily.config.FeishuProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class FeishuCrypto {

    private final FeishuProperties properties;
    private final ObjectMapper objectMapper;

    public FeishuCrypto(FeishuProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean encryptionEnabled() {
        return properties.encryptionEnabled();
    }

    /**
     * 飞书加密事件：请求体为 {"encrypt":"..."}，解密后才是真实 JSON。
     */
    public String decryptIfNeeded(String rawBody) throws Exception {
        if (!properties.encryptionEnabled()) {
            return rawBody;
        }
        JsonNode root = objectMapper.readTree(rawBody);
        if (!root.has("encrypt")) {
            return rawBody;
        }
        String encrypt = root.get("encrypt").asText();
        return decrypt(encrypt, properties.getEncryptKey());
    }

    /**
     * URL 验证回包：开启 Encrypt Key 时，需将 challenge 加密后放入 encrypt 字段。
     */
    public String encryptChallenge(String challenge) throws Exception {
        return encrypt(challenge, properties.getEncryptKey());
    }

    private static String decrypt(String base64, String encryptKey) throws Exception {
        byte[] key = sha256Key(encryptKey);
        byte[] buf = Base64.getDecoder().decode(base64);
        byte[] iv = Arrays.copyOfRange(buf, 0, 16);
        byte[] data = Arrays.copyOfRange(buf, 16, buf.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] plain = cipher.doFinal(data);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private static String encrypt(String plain, String encryptKey) throws Exception {
        byte[] key = sha256Key(encryptKey);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    private static byte[] sha256Key(String encryptKey) throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(encryptKey.getBytes(StandardCharsets.UTF_8));
    }
}
