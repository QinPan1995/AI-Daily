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
        if (timestamp == null || nonce == null || signature == null || rawBody == null) {
            return false;
        }
        try {
            String base = timestamp + nonce + properties.getEncryptKey() + rawBody;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(hash);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
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
