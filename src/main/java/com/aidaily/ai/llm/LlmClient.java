package com.aidaily.ai.llm;

/**
 * 大模型调用抽象，便于后续接入 OpenAI、Claude 等。
 */
public interface LlmClient {

    String getProviderName();

    /**
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return 模型原始文本响应
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 生成自然语言内容（如日报），不要求 JSON 格式。
     */
    String chatText(String systemPrompt, String userPrompt);
}
