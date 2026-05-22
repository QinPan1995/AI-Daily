ALTER TABLE message_raw
    ADD COLUMN ai_processed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已完成 AI 分析',
    ADD COLUMN ai_error VARCHAR(500) NULL COMMENT 'AI 分析失败原因';

CREATE TABLE work_event (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    message_raw_id  BIGINT       NULL COMMENT '来源消息',
    project_name    VARCHAR(100) NULL,
    event_type      VARCHAR(50)  NULL,
    summary         TEXT         NULL,
    status          VARCHAR(50)  NULL,
    event_time      DATETIME(3)  NOT NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_event_time (event_time),
    KEY idx_project_name (project_name),
    KEY idx_message_raw_id (message_raw_id),
    CONSTRAINT fk_work_event_message_raw
        FOREIGN KEY (message_raw_id) REFERENCES message_raw (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
