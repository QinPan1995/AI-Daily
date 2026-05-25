package com.aidaily.config;

import com.aidaily.ai.llm.LlmClient;
import com.aidaily.ai.llm.OpenAiCompatibleLlmClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class LlmConfig {

    @Bean
    public RestTemplate llmRestTemplate(AiProperties aiProperties) {
        int timeoutMs = aiProperties.getActiveTimeoutMs();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek", matchIfMissing = true)
    public LlmClient deepSeekLlmClient(AiProperties aiProperties, RestTemplate llmRestTemplate) {
        AiProperties.DeepSeek cfg = aiProperties.getDeepseek();
        return new OpenAiCompatibleLlmClient(
                "deepseek",
                cfg.getBaseUrl(),
                cfg.getModel(),
                cfg.getApiKey(),
                true,
                llmRestTemplate
        );
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
    public LlmClient ollamaLlmClient(AiProperties aiProperties, RestTemplate llmRestTemplate) {
        AiProperties.Ollama cfg = aiProperties.getOllama();
        return new OpenAiCompatibleLlmClient(
                "ollama",
                cfg.getBaseUrl(),
                cfg.getModel(),
                cfg.getApiKey(),
                false,
                llmRestTemplate
        );
    }
}
