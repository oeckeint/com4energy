package com.com4energy.recordsapi.controller.filerecords.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FileRecordListItemResponse(
        long id,
        String originalFilename,
        String finalFilename,
        String type,
        String status,
        String qualityStatus,
        String origin,
        int retryCount,
        Integer processedRecords,
        Integer defectedRecords,
        Long processingDurationMs,
        LocalDateTime uploadedAt,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
}

