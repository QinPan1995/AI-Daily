package com.aidaily.repository;

import com.aidaily.entity.MessageRaw;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRawRepository extends JpaRepository<MessageRaw, Long> {

    boolean existsByMessageId(String messageId);

    java.util.List<MessageRaw> findByAiProcessedFalseOrderBySendTimeAsc();
}
