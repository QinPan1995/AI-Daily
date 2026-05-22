package com.aidaily.ai;

import com.aidaily.domain.WorkEventType;

public final class WorkEventPrompt {

    private WorkEventPrompt() {
    }

    public static final String SYSTEM = ""
            + "你是一个工作记录分析助手。从飞书聊天内容中提取真实的工作事件，忽略闲聊和无效消息。\n"
            + "规则：\n"
            + "1. 只提取有明确工作含义的内容（任务完成、进行中、阻塞、计划等）\n"
            + "2. 忽略：收到、好的、ok、哈哈、？、纯表情、无意义短语\n"
            + "3. 尽量识别项目名称 projectName\n"
            + "4. eventType 只能是英文编码（括号内为中文含义）：" + eventTypeDescription() + "\n"
            + "5. status 只能是：DONE, IN_PROGRESS, BLOCKED, OPEN\n"
            + "6. 若无工作事件，返回空数组 events: []\n"
            + "7. 只输出 JSON，格式：{\"events\":[{\"projectName\":\"\",\"eventType\":\"\",\"summary\":\"\",\"status\":\"\",\"nextAction\":\"\"}]}\n"
            + "8. nextAction 可选，无则留空字符串";

    private static String eventTypeDescription() {
        StringBuilder sb = new StringBuilder();
        for (WorkEventType type : WorkEventType.values()) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(type.getCode()).append("（").append(type.getLabelZh()).append("）");
        }
        return sb.toString();
    }
}
