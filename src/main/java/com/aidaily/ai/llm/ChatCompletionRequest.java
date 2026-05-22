package com.aidaily.ai.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ChatCompletionRequest {

    private String model;
    private List<ChatMessage> messages;
    private double temperature = 0.1;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {
        }

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class ResponseFormat {
        private String type = "json_object";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
