package com.aidaily.controller;

import com.aidaily.service.FeishuEventService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/feishu")
public class FeishuEventController {

    private final FeishuEventService feishuEventService;

    public FeishuEventController(FeishuEventService feishuEventService) {
        this.feishuEventService = feishuEventService;
    }

    /**
     * 飞书事件订阅回调地址（Request URL）。
     * 需在开放平台配置：接收消息 im.message.receive_v1
     */
    @PostMapping(value = "/event", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> receiveEvent(
            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Lark-Request-Nonce", required = false) String nonce,
            @RequestHeader(value = "X-Lark-Signature", required = false) String signature,
            @RequestBody String rawBody) throws Exception {
        return feishuEventService.handle(timestamp, nonce, signature, rawBody);
    }
}
