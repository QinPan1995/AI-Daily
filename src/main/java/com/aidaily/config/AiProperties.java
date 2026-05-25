package com.aidaily.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 模型提供商：deepseek | ollama */
    private String provider = "deepseek";

    private boolean enabled = true;

    private final DeepSeek deepseek = new DeepSeek();
    private final Ollama ollama = new Ollama();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DeepSeek getDeepseek() {
        return deepseek;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public boolean isLlmConfigured() {
        if (!enabled) {
            return false;
        }
        if (isOllamaProvider()) {
            return ollama.isConfigured();
        }
        return deepseek.isConfigured();
    }

    public int getActiveTimeoutMs() {
        if (isOllamaProvider()) {
            return ollama.getTimeoutMs();
        }
        return deepseek.getTimeoutMs();
    }

    public boolean isOllamaProvider() {
        return "ollama".equalsIgnoreCase(provider);
    }

    public static class DeepSeek {

        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";
        private int timeoutMs = 60000;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.trim().isEmpty();
        }
    }

    public static class Ollama {

        private String baseUrl = "http://localhost:11434";
        private String model = "qwen2.5:7b";
        private int timeoutMs = 120000;
        /** 本地一般无需 key；若通过网关代理可填写 */
        private String apiKey;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isConfigured() {
            return baseUrl != null && !baseUrl.trim().isEmpty()
                    && model != null && !model.trim().isEmpty();
        }
    }
}
