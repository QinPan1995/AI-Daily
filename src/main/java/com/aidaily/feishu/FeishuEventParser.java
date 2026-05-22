package com.aidaily.feishu;

import com.aidaily.domain.ChatType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class FeishuEventParser {

    private final ObjectMapper objectMapper;

    public FeishuEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<UrlVerification> parseUrlVerification(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        if (root.has("challenge") && root.has("type")) {
            String challenge = root.get("challenge").asText();
            String type = root.get("type").asText();
            if ("url_verification".equals(type)) {
                return Optional.of(new UrlVerification(challenge));
            }
            return Optional.empty();
        }
        return Optional.empty();
    }

    public Optional<IncomingMessage> parseMessageEvent(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String eventType = textOrNull(root.path("header").path("event_type"));
        if (eventType == null) {
            eventType = textOrNull(root.path("event").path("type"));
        }
        if (!"im.message.receive_v1".equals(eventType)) {
            return Optional.empty();
        }

        JsonNode event = root.path("event");
        JsonNode message = event.path("message");
        if (message.isMissingNode()) {
            return Optional.empty();
        }

        String messageId = textOrNull(message.path("message_id"));
        String chatId = textOrNull(message.path("chat_id"));
        if (messageId == null || chatId == null) {
            return Optional.empty();
        }

        String chatTypeRaw = message.path("chat_type").asText("group");
        ChatType chatType = ChatType.fromFeishuChatType(chatTypeRaw);

        String senderOpenId = textOrNull(event.path("sender").path("sender_id").path("open_id"));
        String messageType = message.path("message_type").asText("text");
        String content = extractTextContent(messageType, message.path("content").asText("{}"));

        long createTimeMs = message.path("create_time").asLong(0);
        Instant sendTime = createTimeMs > 0
                ? Instant.ofEpochMilli(createTimeMs)
                : Instant.now();

        return Optional.of(new IncomingMessage(
                messageId,
                senderOpenId,
                chatId,
                chatType,
                messageType,
                content,
                sendTime,
                body
        ));
    }

    private String extractTextContent(String messageType, String contentJson) {
        if (!"text".equals(messageType)) {
            return "[" + messageType + "] " + contentJson;
        }
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            return node.path("text").asText(contentJson);
        } catch (Exception e) {
            return contentJson;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text.isEmpty() ? null : text;
    }

    public static class UrlVerification {
        private final String challenge;

        public UrlVerification(String challenge) {
            this.challenge = challenge;
        }

        public String getChallenge() {
            return challenge;
        }

        @Override
        public String toString() {
            return "UrlVerification{" +
                    "challenge='" + challenge + '\'' +
                    '}';
        }
    }

    public static class IncomingMessage {
        private final String messageId;
        private final String senderOpenId;
        private final String chatId;
        private final ChatType chatType;
        private final String messageType;
        private final String content;
        private final Instant sendTime;
        private final String rawPayload;

        public IncomingMessage(String messageId, String senderOpenId, String chatId, ChatType chatType,
                               String messageType, String content, Instant sendTime, String rawPayload) {
            this.messageId = messageId;
            this.senderOpenId = senderOpenId;
            this.chatId = chatId;
            this.chatType = chatType;
            this.messageType = messageType;
            this.content = content;
            this.sendTime = sendTime;
            this.rawPayload = rawPayload;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getSenderOpenId() {
            return senderOpenId;
        }

        public String getChatId() {
            return chatId;
        }

        public ChatType getChatType() {
            return chatType;
        }

        public String getMessageType() {
            return messageType;
        }

        public String getContent() {
            return content;
        }

        public Instant getSendTime() {
            return sendTime;
        }

        public String getRawPayload() {
            return rawPayload;
        }
    }
}
