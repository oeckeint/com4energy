package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.domain.entity.messaging.FileRecordEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRecordEventRepository extends JpaRepository<FileRecordEvent, Long> {
}

