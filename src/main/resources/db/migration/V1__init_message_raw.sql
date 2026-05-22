CREATE TABLE message_raw (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    message_id      VARCHAR(64)  NOT NULL,
    sender_open_id  VARCHAR(64)  NULL,
    sender_name     VARCHAR(100) NULL,
    chat_id         VARCHAR(64)  NOT NULL,
    chat_name       VARCHAR(200) NULL,
    chat_type       VARCHAR(20)  NOT NULL COMMENT 'GROUP | PRIVATE',
    message_type    VARCHAR(32)  NULL,
    content         TEXT         NULL,
    send_time       DATETIME(3)  NOT NULL,
    raw_payload     JSON         NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_send_time (send_time),
    KEY idx_chat_type_time (chat_type, send_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
