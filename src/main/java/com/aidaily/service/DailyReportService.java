package com.aidaily.service;

import com.aidaily.ai.DailyReportPrompt;
import com.aidaily.ai.MessageNoiseFilter;
import com.aidaily.ai.llm.LlmClient;
import com.aidaily.config.AiProperties;
import com.aidaily.dto.DailyReportGenerateResponse;
import com.aidaily.entity.DailyReport;
import com.aidaily.entity.MessageRaw;
import com.aidaily.repository.DailyReportRepository;
import com.aidaily.repository.MessageRawRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DailyReportService {

    private static final Logger log = LoggerFactory.getLogger(DailyReportService.class);
    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AiProperties aiProperties;
    private final LlmClient llmClient;
    private final MessageRawRepository messageRawRepository;
    private final DailyReportRepository dailyReportRepository;
    private final MessageNoiseFilter noiseFilter;

    public DailyReportService(
            AiProperties aiProperties,
            LlmClient llmClient,
            MessageRawRepository messageRawRepository,
            DailyReportRepository dailyReportRepository,
            MessageNoiseFilter noiseFilter) {
        this.aiProperties = aiProperties;
        this.llmClient = llmClient;
        this.messageRawRepository = messageRawRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.noiseFilter = noiseFilter;
    }

    @Transactional(readOnly = true)
    public DailyReport getByDate(LocalDate reportDate) {
        return dailyReportRepository.findByReportDate(reportDate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "日报不存在: " + reportDate));
    }

    @Transactional
    public DailyReportGenerateResponse generate(LocalDate reportDate) {
        if (reportDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reportDate 不能为空");
        }
        if (!aiProperties.isLlmConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 未配置或已禁用");
        }

        Instant start = reportDate.atStartOfDay(ZONE_SHANGHAI).toInstant();
        Instant end = reportDate.plusDays(1).atStartOfDay(ZONE_SHANGHAI).toInstant();
        List<MessageRaw> allMessages = messageRawRepository.findBySendTimeRangeOrderBySendTimeAsc(start, end);
        List<MessageRaw> messages = filterMeaningfulMessages(allMessages);

        log.info("Generate daily report date={} rawCount={} promptCount={}",
                reportDate, allMessages.size(), messages.size());

        String userPrompt = buildUserPrompt(reportDate, messages);
        String content = llmClient.chatText(DailyReportPrompt.SYSTEM, userPrompt);

        Optional<DailyReport> existing = dailyReportRepository.findByReportDate(reportDate);
        boolean regenerated = existing.isPresent();
        DailyReport report = existing.orElseGet(DailyReport::new);
        report.setReportDate(reportDate);
        report.setContent(content);
        DailyReport saved = dailyReportRepository.save(report);

        DailyReportGenerateResponse response = new DailyReportGenerateResponse();
        response.setId(saved.getId());
        response.setReportDate(saved.getReportDate());
        response.setContent(saved.getContent());
        response.setMessageCount(messages.size());
        response.setRawMessageCount(allMessages.size());
        response.setRegenerated(regenerated);
        return response;
    }

    private List<MessageRaw> filterMeaningfulMessages(List<MessageRaw> allMessages) {
        List<MessageRaw> result = new ArrayList<>();
        for (MessageRaw message : allMessages) {
            if (!noiseFilter.isNoise(message.getContent())) {
                result.add(message);
            }
        }
        return result;
    }

    private String buildUserPrompt(LocalDate reportDate, List<MessageRaw> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("日报日期: ").append(reportDate).append('\n');
        sb.append("时区: Asia/Shanghai\n");
        sb.append("说明: 以下为当日飞书原始聊天记录（按时间升序），请从中提炼工作相关内容。\n");
        sb.append("聊天记录:\n");
        if (messages.isEmpty()) {
            sb.append("（无有效聊天内容）\n");
        } else {
            int index = 1;
            for (MessageRaw message : messages) {
                sb.append(index++).append(". ");
                sb.append(formatTime(message.getSendTime())).append(' ');
                sb.append('[').append(message.getChatType()).append("] ");
                if (message.getChatName() != null && !message.getChatName().isEmpty()) {
                    sb.append("会话:").append(message.getChatName()).append(' ');
                } else if (message.getChatId() != null) {
                    sb.append("chatId:").append(message.getChatId()).append(' ');
                }
                if (message.getSenderName() != null && !message.getSenderName().isEmpty()) {
                    sb.append("发送者:").append(message.getSenderName()).append(' ');
                } else if (message.getSenderOpenId() != null) {
                    sb.append("发送者:").append(message.getSenderOpenId()).append(' ');
                }
                sb.append("内容:").append(message.getContent() != null ? message.getContent() : "");
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String formatTime(Instant instant) {
        if (instant == null) {
            return "--:--";
        }
        return TIME_FMT.format(ZonedDateTime.ofInstant(instant, ZONE_SHANGHAI));
    }
}
