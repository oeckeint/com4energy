package com.com4energy.recordsapi.service.dto;

import java.time.LocalDateTime;

public record FileRecordQueryCriteria(
        int page,
        int size,
        String sortBy,
        String sortDir,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String origin,
        String status,
        String fileType,
        String filename
) {
}

