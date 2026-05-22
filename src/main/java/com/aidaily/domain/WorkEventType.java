package com.aidaily.domain;

/**
 * AI 提取的工作事件类型。
 */
public enum WorkEventType {

    TASK_DONE("任务完成"),
    TASK_STARTED("任务开始"),
    TASK_IN_PROGRESS("任务进行中"),
    BLOCKER("阻塞/风险"),
    PLANNED("计划/待办"),
    INFO("信息同步");

    private final String labelZh;

    WorkEventType(String labelZh) {
        this.labelZh = labelZh;
    }

    /** 中文描述，用于展示 */
    public String getLabelZh() {
        return labelZh;
    }

    /** 存储与 API 使用的英文编码 */
    public String getCode() {
        return name();
    }

    /**
     * 根据英文 code 或中文 label 解析枚举。
     */
    public static WorkEventType fromCode(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return WorkEventType.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // 尝试按中文匹配
        }
        for (WorkEventType type : values()) {
            if (type.labelZh.equals(trimmed)) {
                return type;
            }
        }
        return null;
    }
}
