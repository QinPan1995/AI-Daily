package com.aidaily.repository;

import com.aidaily.entity.WorkEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkEventRepository extends JpaRepository<WorkEvent, Long> {

    List<WorkEvent> findByMessageRawId(Long messageRawId);
}
