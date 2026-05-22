ALTER TABLE work_event
    ADD COLUMN event_type_label VARCHAR(50) NULL COMMENT '事件类型中文' AFTER event_type;
