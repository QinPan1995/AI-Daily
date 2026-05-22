package com.aidaily.dto;

import java.time.LocalDate;

public class DailyReportGenerateResponse {

    private Long id;
    private LocalDate reportDate;
    private String content;
    /** 送入 AI 的有效消息条数（已过滤噪声） */
    private int messageCount;
    /** 当日原始消息总条数 */
    private int rawMessageCount;
    private boolean regenerated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getRawMessageCount() {
        return rawMessageCount;
    }

    public void setRawMessageCount(int rawMessageCount) {
        this.rawMessageCount = rawMessageCount;
    }

    public boolean isRegenerated() {
        return regenerated;
    }

    public void setRegenerated(boolean regenerated) {
        this.regenerated = regenerated;
    }

    /** @deprecated 使用 {@link #getMessageCount()} */
    @Deprecated
    public int getEventCount() {
        return messageCount;
    }

    /** @deprecated 使用 {@link #setMessageCount(int)} */
    @Deprecated
    public void setEventCount(int eventCount) {
        this.messageCount = eventCount;
    }
}
