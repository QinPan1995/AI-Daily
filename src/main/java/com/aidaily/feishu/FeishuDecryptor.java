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
import java.util.Arrays;
import java.util.Base64;

@Component
public class FeishuDecryptor {

    private final FeishuProperties properties;
    private final ObjectMapper objectMapper;

    public FeishuDecryptor(FeishuProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
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

    private static String decrypt(String base64, String encryptKey) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(encryptKey.getBytes(StandardCharsets.UTF_8));
        byte[] buf = Base64.getDecoder().decode(base64);
        byte[] iv = Arrays.copyOfRange(buf, 0, 16);
        byte[] data = Arrays.copyOfRange(buf, 16, buf.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] plain = cipher.doFinal(data);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
