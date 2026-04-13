package com.com4energy.recordsapi.controller.filerecords.dto;

import lombok.Builder;

@Builder
public record FileRecordMetricsSummaryResponse(
        long totalFiles,
        long totalProcessedRecords,
        long totalDefectedRecords,
        long avgProcessingDurationMs,
        long minProcessingDurationMs,
        long maxProcessingDurationMs
) {
}

