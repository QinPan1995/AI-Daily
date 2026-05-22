package com.aidaily.controller;

import com.aidaily.dto.DailyReportGenerateResponse;
import com.aidaily.entity.DailyReport;
import com.aidaily.service.DailyReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/daily-reports")
public class DailyReportController {

    private final DailyReportService dailyReportService;

    public DailyReportController(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    /**
     * 查询指定日期已生成的日报。
     * 示例: GET /api/daily-reports?date=2026-05-21
     */
    @GetMapping
    public DailyReport get(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyReportService.getByDate(date);
    }

    /**
     * 查询指定日期原始消息（按时间排序），一次 AI 调用生成日报并入库。
     * 同一天重复调用会覆盖更新 content。
     * 示例: POST /api/daily-reports/generate?date=2026-05-21
     */
    @PostMapping("/generate")
    public DailyReportGenerateResponse generate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyReportService.generate(date);
    }
}
