package com.aidaily.service;

import com.aidaily.feishu.FeishuCrypto;
import com.aidaily.feishu.FeishuEventParser;
import com.aidaily.feishu.FeishuSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FeishuEventService {

    private static final Logger log = LoggerFactory.getLogger(FeishuEventService.class);

    private final FeishuSignatureVerifier signatureVerifier;
    private final FeishuCrypto feishuCrypto;
    private final FeishuEventParser eventParser;
    private final MessageCollectorService messageCollectorService;
    private final ObjectMapper objectMapper;

    public FeishuEventService(
            FeishuSignatureVerifier signatureVerifier,
            FeishuCrypto feishuCrypto,
            FeishuEventParser eventParser,
            MessageCollectorService messageCollectorService,
            ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.feishuCrypto = feishuCrypto;
        this.eventParser = eventParser;
        this.messageCollectorService = messageCollectorService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> handle(
            String timestamp,
            String nonce,
            String signature,
            String rawBody) throws Exception {
        // 记录所有参数
        log.info("Feishu event received: timestamp={}, nonce={}, signature={}, rawBody={}", timestamp, nonce, signature, rawBody);
        if (!signatureVerifier.verifyEncrypted(timestamp, nonce, signature, rawBody)) {
            log.warn("Feishu signature verification failed");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Feishu signature");
        }

        String body = feishuCrypto.decryptIfNeeded(rawBody);

        // URL 验证优先处理：未加密时必须校验 body 内 token；加密时回包也需加密 challenge
        Optional<FeishuEventParser.UrlVerification> urlVerification = eventParser.parseUrlVerification(body);
        if (urlVerification.isPresent()) {
            if (!feishuCrypto.encryptionEnabled()
                    && !signatureVerifier.verifyPlainToken(extractToken(body))) {
                log.warn("Feishu URL verification token mismatch");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification token");
            }
            return buildChallengeResponse(urlVerification.get().getChallenge());
        }

        if (!feishuCrypto.encryptionEnabled()
                && !signatureVerifier.verifyPlainToken(extractToken(body))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification token");
        }

        Optional<FeishuEventParser.IncomingMessage> message = eventParser.parseMessageEvent(body);
        if (message.isPresent()) {
            messageCollectorService.saveIfNew(message.get());
        }

        ObjectNode ok = objectMapper.createObjectNode();
        ok.put("code", 0);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.convertValue(ok, Map.class);
        return result;
    }

    /**
     * 未开加密：{"challenge":"xxx"}
     * 已开加密：{"encrypt":"加密后的 challenge"}（飞书文档要求，否则提示 Challenge 未返回）
     */
    private Map<String, Object> buildChallengeResponse(String challenge) throws Exception {
        if (feishuCrypto.encryptionEnabled()) {
            String encrypted = feishuCrypto.encryptChallenge(challenge);
            Map<String, Object> resp = new HashMap<String, Object>();
            resp.put("encrypt", encrypted);
            log.info("Feishu URL verification OK (encrypted challenge returned)");
            return resp;
        }
        log.info("Feishu URL verification OK (plain challenge returned)");
        return Collections.<String, Object>singletonMap("challenge", challenge);
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
