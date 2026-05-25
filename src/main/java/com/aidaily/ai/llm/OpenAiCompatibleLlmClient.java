package com.aidaily.ai.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * 调用 OpenAI 兼容的 Chat Completions API（DeepSeek、Ollama 等）。
 */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final String providerName;
    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final boolean jsonResponseFormat;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleLlmClient(
            String providerName,
            String baseUrl,
            String model,
            String apiKey,
            boolean jsonResponseFormat,
            RestTemplate restTemplate) {
        this.providerName = providerName;
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.jsonResponseFormat = jsonResponseFormat;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return providerName;
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
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(model);
        request.setTemperature(jsonMode ? 0.1 : 0.3);
        request.setMessages(Arrays.asList(
                new ChatCompletionRequest.ChatMessage("system", systemPrompt),
                new ChatCompletionRequest.ChatMessage("user", userPrompt)
        ));
        if (jsonMode && jsonResponseFormat) {
            ChatCompletionRequest.ResponseFormat format = new ChatCompletionRequest.ResponseFormat();
            format.setType("json_object");
            request.setResponseFormat(format);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.setBearerAuth(apiKey.trim());
        }

        String url = trimTrailingSlash(baseUrl) + "/v1/chat/completions";
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<ChatCompletionRequest>(request, headers);

        log.debug("Calling {} API: {}", providerName, url);
        ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                url, entity, ChatCompletionResponse.class);

        ChatCompletionResponse body = response.getBody();
        if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
            throw new IllegalStateException(providerName + " API returned empty response");
        }
        ChatCompletionRequest.ChatMessage message = body.getChoices().get(0).getMessage();
        if (message == null || message.getContent() == null) {
            throw new IllegalStateException(providerName + " API returned empty message content");
        }
        return message.getContent();
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("LLM base-url is not configured");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
