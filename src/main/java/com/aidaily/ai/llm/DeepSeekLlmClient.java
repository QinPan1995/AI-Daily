package com.aidaily.ai.llm;

import com.aidaily.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

public class DeepSeekLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekLlmClient.class);

    private final AiProperties.DeepSeek config;
    private final RestTemplate restTemplate;

    public DeepSeekLlmClient(AiProperties.DeepSeek config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return "deepseek";
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, true);
    }

    @Override
    public String chatText(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, false);
    }

    private String complete(String systemPrompt, String userPrompt, boolean jsonMode) {
        if (!config.isConfigured()) {
            throw new IllegalStateException("DeepSeek API key is not configured");
        }

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(config.getModel());
        request.setTemperature(jsonMode ? 0.1 : 0.3);
        request.setMessages(Arrays.asList(
                new ChatCompletionRequest.ChatMessage("system", systemPrompt),
                new ChatCompletionRequest.ChatMessage("user", userPrompt)
        ));
        if (jsonMode) {
            ChatCompletionRequest.ResponseFormat format = new ChatCompletionRequest.ResponseFormat();
            format.setType("json_object");
            request.setResponseFormat(format);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        String url = trimTrailingSlash(config.getBaseUrl()) + "/v1/chat/completions";
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<ChatCompletionRequest>(request, headers);

        log.debug("Calling DeepSeek API: {}", url);
        ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                url, entity, ChatCompletionResponse.class);

        ChatCompletionResponse body = response.getBody();
        if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
            throw new IllegalStateException("DeepSeek API returned empty response");
        }
        ChatCompletionRequest.ChatMessage message = body.getChoices().get(0).getMessage();
        if (message == null || message.getContent() == null) {
            throw new IllegalStateException("DeepSeek API returned empty message content");
        }
        return message.getContent();
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null) {
            return "https://api.deepseek.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
