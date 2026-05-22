package com.aidaily.service;

import com.aidaily.feishu.FeishuDecryptor;
import com.aidaily.feishu.FeishuEventParser;
import com.aidaily.feishu.FeishuSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class FeishuEventService {

    private final FeishuSignatureVerifier signatureVerifier;
    private final FeishuDecryptor decryptor;
    private final FeishuEventParser eventParser;
    private final MessageCollectorService messageCollectorService;
    private final ObjectMapper objectMapper;

    public FeishuEventService(
            FeishuSignatureVerifier signatureVerifier,
            FeishuDecryptor decryptor,
            FeishuEventParser eventParser,
            MessageCollectorService messageCollectorService,
            ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.decryptor = decryptor;
        this.eventParser = eventParser;
        this.messageCollectorService = messageCollectorService;
        this.objectMapper = objectMapper;
    }

    public Map<String, String> handle(
            String timestamp,
            String nonce,
            String signature,
            String rawBody) throws Exception {

        if (!signatureVerifier.verifyEncrypted(timestamp, nonce, signature, rawBody)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Feishu signature");
        }

        String body = decryptor.decryptIfNeeded(rawBody);

        if (!signatureVerifier.isEncryptionEnabled()
                && !signatureVerifier.verifyPlainToken(extractToken(body))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification token");
        }

        Optional<FeishuEventParser.UrlVerification> urlVerification = eventParser.parseUrlVerification(body);
        if (urlVerification.isPresent()) {
            return Collections.singletonMap("challenge", urlVerification.get().getChallenge());
        }

        Optional<FeishuEventParser.IncomingMessage> message = eventParser.parseMessageEvent(body);
        if (message.isPresent()) {
            messageCollectorService.saveIfNew(message.get());
        }

        ObjectNode ok = objectMapper.createObjectNode();
        ok.put("code", 0);
        @SuppressWarnings("unchecked")
        Map<String, String> result = objectMapper.convertValue(ok, Map.class);
        return result;
    }

    private String extractToken(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode headerToken = root.path("header").path("token");
        if (!headerToken.isMissingNode() && !headerToken.isNull()) {
            String token = headerToken.asText();
            if (!token.isEmpty()) {
                return token;
            }
        }
        JsonNode rootToken = root.path("token");
        if (!rootToken.isMissingNode() && !rootToken.isNull()) {
            return rootToken.asText();
        }
        return null;
    }
}
