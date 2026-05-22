package com.aidaily.config;

import com.aidaily.ai.llm.DeepSeekLlmClient;
import com.aidaily.ai.llm.LlmClient;
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
        int timeoutMs = aiProperties.getDeepseek().getTimeoutMs();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek", matchIfMissing = true)
    public LlmClient deepSeekLlmClient(AiProperties aiProperties, RestTemplate llmRestTemplate) {
        return new DeepSeekLlmClient(aiProperties.getDeepseek(), llmRestTemplate);
    }
}
