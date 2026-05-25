package com.com4energy.recordsapi.controller.notifications.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FileProcessingNotificationResponse(
        Long id,
        String eventType,
        String status,
        String filename,
        String fileType,
        String origin,
        String failureReason,
        String comment,
        LocalDateTime occurredAt,
        LocalDateTime receivedAt
) {
}

