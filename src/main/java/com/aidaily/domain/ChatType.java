package com.aidaily.domain;

/**
 * MVP 区分群聊与私聊；飞书 topic 线程归入群聊。
 */
public enum ChatType {
    GROUP,
    PRIVATE;

    public static ChatType fromFeishuChatType(String feishuChatType) {
        if (feishuChatType == null) {
            return GROUP;
        }
        String type = feishuChatType.toLowerCase();
        if ("p2p".equals(type)) {
            return PRIVATE;
        }
        if ("group".equals(type) || "topic".equals(type)) {
            return GROUP;
        }
        return GROUP;
    }
}
