package com.com4energy.recordsapi.service.dto;

import java.time.LocalDateTime;

public record FileRecordEventQueryCriteria(
        int page,
        int size,
        String sortBy,
        String sortDir,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String eventType,
        String status,
        String origin,
        String fileType,
        String filename
) {
}

