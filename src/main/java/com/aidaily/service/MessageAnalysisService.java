package com.aidaily.service;

import com.aidaily.ai.LlmResponseJsonParser;
import com.aidaily.ai.MessageNoiseFilter;
import com.aidaily.ai.WorkEventPrompt;
import com.aidaily.ai.dto.ExtractedWorkEventDto;
import com.aidaily.ai.dto.WorkEventExtractionResult;
import com.aidaily.ai.llm.LlmClient;
import com.aidaily.config.AiProperties;
import com.aidaily.domain.WorkEventType;
import com.aidaily.entity.MessageRaw;
import com.aidaily.entity.WorkEvent;
import com.aidaily.repository.MessageRawRepository;
import com.aidaily.repository.WorkEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class MessageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MessageAnalysisService.class);
    private static final int AI_ERROR_MAX_LEN = 500;

    private final AiProperties aiProperties;
    private final LlmClient llmClient;
    private final MessageRawRepository messageRawRepository;
    private final WorkEventRepository workEventRepository;
    private final MessageNoiseFilter noiseFilter;
    private final LlmResponseJsonParser jsonParser;

    public MessageAnalysisService(
            AiProperties aiProperties,
            LlmClient llmClient,
            MessageRawRepository messageRawRepository,
            WorkEventRepository workEventRepository,
            MessageNoiseFilter noiseFilter,
            ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.llmClient = llmClient;
        this.messageRawRepository = messageRawRepository;
        this.workEventRepository = workEventRepository;
        this.noiseFilter = noiseFilter;
        this.jsonParser = new LlmResponseJsonParser(objectMapper);
    }

    @Async
    public void analyzeAsync(Long messageRawId) {
        try {
            analyze(messageRawId);
        } catch (Exception e) {
            log.error("Async AI analysis failed for messageRawId={}", messageRawId, e);
        }
    }

    @Transactional
    public void analyze(Long messageRawId) {
        MessageRaw message = messageRawRepository.findById(messageRawId).orElse(null);
        if (message == null) {
            log.warn("Message not found for analysis: {}", messageRawId);
            return;
        }
        if (message.isAiProcessed()) {
            log.debug("Message already processed: {}", messageRawId);
            return;
        }

        if (!aiProperties.isLlmConfigured()) {
            log.warn("AI analysis skipped: disabled or LLM provider not configured");
            markProcessed(message, null);
            return;
        }

        String content = message.getContent();
        if (noiseFilter.isNoise(content)) {
            log.info("Skip noise message id={} content={}", messageRawId, content);
            markProcessed(message, null);
            return;
        }

        try {
            String userPrompt = buildUserPrompt(message);
            String llmResponse = llmClient.chat(WorkEventPrompt.SYSTEM, userPrompt);
            WorkEventExtractionResult result = jsonParser.parse(llmResponse);
            List<ExtractedWorkEventDto> events = result.getEvents() != null
                    ? result.getEvents() : Collections.emptyList();

            for (ExtractedWorkEventDto dto : events) {
                if (isEmptyEvent(dto)) {
                    continue;
                }
                workEventRepository.save(toEntity(dto, message));
            }

            markProcessed(message, null);
            log.info("AI analysis done messageRawId={} events={}", messageRawId, events.size());
        } catch (Exception e) {
            log.error("AI analysis error messageRawId={}", messageRawId, e);
            markFailed(message, e.getMessage());
        }
    }

    private String buildUserPrompt(MessageRaw message) {
        StringBuilder sb = new StringBuilder();
        sb.append("聊天类型: ").append(message.getChatType()).append('\n');
        if (message.getChatName() != null) {
            sb.append("会话: ").append(message.getChatName()).append('\n');
        }
        sb.append("发送者: ").append(message.getSenderOpenId() != null ? message.getSenderOpenId() : "未知").append('\n');
        sb.append("发送时间: ").append(message.getSendTime()).append('\n');
        sb.append("消息内容:\n").append(message.getContent());
        return sb.toString();
    }

    private WorkEvent toEntity(ExtractedWorkEventDto dto, MessageRaw message) {
        WorkEvent event = new WorkEvent();
        event.setMessageRawId(message.getId());
        event.setProjectName(trimToLength(dto.getProjectName(), 100));
        WorkEventType eventType = WorkEventType.fromCode(dto.getEventType());
        if (eventType != null) {
            event.setEventType(eventType.getCode());
            event.setEventTypeLabel(eventType.getLabelZh());
        } else {
            event.setEventType(trimToLength(dto.getEventType(), 50));
            event.setEventTypeLabel(null);
        }
        event.setSummary(buildSummary(dto));
        event.setStatus(trimToLength(dto.getStatus(), 50));
        event.setEventTime(message.getSendTime() != null ? message.getSendTime() : Instant.now());
        return event;
    }

    private String buildSummary(ExtractedWorkEventDto dto) {
        String summary = dto.getSummary() != null ? dto.getSummary().trim() : "";
        String next = dto.getNextAction() != null ? dto.getNextAction().trim() : "";
        if (!next.isEmpty()) {
            if (!summary.isEmpty()) {
                return summary + "；下一步：" + next;
            }
            return "下一步：" + next;
        }
        return summary;
    }

    private boolean isEmptyEvent(ExtractedWorkEventDto dto) {
        if (dto == null) {
            return true;
        }
        String summary = dto.getSummary();
        return summary == null || summary.trim().isEmpty();
    }

    private void markProcessed(MessageRaw message, String error) {
        message.setAiProcessed(true);
        message.setAiError(truncateError(error));
        messageRawRepository.save(message);
    }

    private void markFailed(MessageRaw message, String error) {
        message.setAiProcessed(false);
        message.setAiError(truncateError(error));
        messageRawRepository.save(message);
    }

    private String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= AI_ERROR_MAX_LEN ? error : error.substring(0, AI_ERROR_MAX_LEN);
    }

    private static String trimToLength(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
