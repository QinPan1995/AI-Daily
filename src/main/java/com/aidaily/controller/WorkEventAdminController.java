package com.aidaily.controller;

import com.aidaily.entity.WorkEvent;
import com.aidaily.repository.WorkEventRepository;
import com.aidaily.service.MessageAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开发调试用：手动触发分析、查看工作事件。
 */
@RestController
@RequestMapping("/admin")
public class WorkEventAdminController {

    private final MessageAnalysisService messageAnalysisService;
    private final WorkEventRepository workEventRepository;

    public WorkEventAdminController(
            MessageAnalysisService messageAnalysisService,
            WorkEventRepository workEventRepository) {
        this.messageAnalysisService = messageAnalysisService;
        this.workEventRepository = workEventRepository;
    }

    @PostMapping("/analyze/{messageRawId}")
    public Map<String, Object> analyze(@PathVariable Long messageRawId) {
        messageAnalysisService.analyze(messageRawId);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("messageRawId", messageRawId);
        resp.put("status", "ok");
        return resp;
    }

    @GetMapping("/work-events")
    public List<WorkEvent> listWorkEvents() {
        return workEventRepository.findAll();
    }

    @GetMapping("/work-events/by-message/{messageRawId}")
    public List<WorkEvent> listByMessage(@PathVariable Long messageRawId) {
        return workEventRepository.findByMessageRawId(messageRawId);
    }
}
