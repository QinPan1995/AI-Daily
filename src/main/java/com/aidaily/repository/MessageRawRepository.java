package com.aidaily.repository;

import com.aidaily.entity.MessageRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MessageRawRepository extends JpaRepository<MessageRaw, Long> {

    boolean existsByMessageId(String messageId);

    List<MessageRaw> findByAiProcessedFalseOrderBySendTimeAsc();

    @Query("SELECT m FROM MessageRaw m WHERE m.sendTime >= :start AND m.sendTime < :end ORDER BY m.sendTime ASC")
    List<MessageRaw> findBySendTimeRangeOrderBySendTimeAsc(
            @Param("start") Instant start,
            @Param("end") Instant end);
}
