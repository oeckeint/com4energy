package com.com4energy.recordsapi.controller.filerecords.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FileRecordEventListItemResponse(
        long id,
        String sourceId,
        String eventType,
        String status,
        String filename,
        String fileType,
        String origin,
        String failureReason,
        String failureReasonDescription,
        Integer failedLineNumber,
        String createdBy,
        LocalDateTime occurredAt,
        LocalDateTime receivedAt,
        String metadataJson
) {
}

