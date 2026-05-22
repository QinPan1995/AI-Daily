-- 请手动在 MySQL 执行（不使用 Flyway）

CREATE TABLE daily_report (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    report_date DATE   NOT NULL,
    content     TEXT   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
