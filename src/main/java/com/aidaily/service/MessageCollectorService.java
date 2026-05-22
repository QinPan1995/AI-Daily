package com.aidaily.service;

import com.aidaily.entity.MessageRaw;
import com.aidaily.feishu.FeishuEventParser;
import com.aidaily.repository.MessageRawRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageCollectorService {

    private static final Logger log = LoggerFactory.getLogger(MessageCollectorService.class);

    private final MessageRawRepository repository;

    public MessageCollectorService(MessageRawRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveIfNew(FeishuEventParser.IncomingMessage incoming) {
        if (repository.existsByMessageId(incoming.getMessageId())) {
            log.debug("Duplicate message skipped: {}", incoming.getMessageId());
            return;
        }

        MessageRaw entity = new MessageRaw();
        entity.setMessageId(incoming.getMessageId());
        entity.setSenderOpenId(incoming.getSenderOpenId());
        entity.setChatId(incoming.getChatId());
        entity.setChatType(incoming.getChatType());
        entity.setMessageType(incoming.getMessageType());
        entity.setContent(incoming.getContent());
        entity.setSendTime(incoming.getSendTime());
        entity.setRawPayload(incoming.getRawPayload());

        repository.save(entity);
        log.info("Saved message {} chatType={} chatId={}",
                incoming.getMessageId(), incoming.getChatType(), incoming.getChatId());
    }
}
