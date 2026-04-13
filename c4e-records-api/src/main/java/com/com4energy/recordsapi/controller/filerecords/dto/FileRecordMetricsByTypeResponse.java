package com.com4energy.recordsapi.controller.filerecords.dto;

import lombok.Builder;

@Builder
public record FileRecordMetricsByTypeResponse(
        String fileType,
        long totalFiles,
        long totalProcessedRecords,
        long totalDefectedRecords,
        long avgProcessingDurationMs
) {
}

