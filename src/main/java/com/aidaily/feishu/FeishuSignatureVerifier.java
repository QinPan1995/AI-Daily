package com.aidaily.feishu;

import com.aidaily.config.FeishuProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

@Component
public class FeishuSignatureVerifier {

    private final FeishuProperties properties;

    public FeishuSignatureVerifier(FeishuProperties properties) {
        this.properties = properties;
    }

    public boolean isEncryptionEnabled() {
        return properties.encryptionEnabled();
    }

    /** 开启 Encrypt Key 时校验签名（对原始请求体）。 */
    public boolean verifyEncrypted(String timestamp, String nonce, String signature, String rawBody) {
        if (!properties.encryptionEnabled()) {
            return true;
        }
        try {
            String content = timestamp + nonce + properties.getEncryptKey() + rawBody;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(hash);
            return expected.equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        String timestamp=null;
        String nonce=null;
        String signature=null;
        String rawBody=null;
        String content = timestamp + nonce +  rawBody;
        System.out.println(content);
    }

    /** 未加密时校验 header.token。 */
    public boolean verifyPlainToken(String token) {
        return Objects.equals(properties.getVerificationToken(), token);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
