package com.aidaily.ai;

import com.aidaily.ai.dto.WorkEventExtractionResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LlmResponseJsonParser {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public LlmResponseJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WorkEventExtractionResult parse(String llmContent) throws Exception {
        String json = extractJson(llmContent);
        return objectMapper.readValue(json, WorkEventExtractionResult.class);
    }

    private String extractJson(String content) {
        if (content == null) {
            return "{\"events\":[]}";
        }
        String trimmed = content.trim();
        Matcher matcher = JSON_BLOCK.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return trimmed;
    }
}
