package com.com4energy.event.publisher.incident.contract;

import com.com4energy.event.publisher.common.Environment;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Instant;
import java.time.LocalDateTime;

/** Contrato de evento de incidente compartido entre microservicios. */
public record IncidentEvent(
        String id,
        String serviceName,
        Environment environment,
        String endpoint,
        String methodName,
        String httpMethod,
        String traceId,
        String spanId,
        String userId,
        String exceptionType,
        String message,
        String stackTrace,
        IncidentCategory category,
        IncidentSeverity severity,
        IncidentStatus status,
        String errorCode,
        String filename,
        String fileType,
        String finalPath,
        String metadata,
        String createdBy,
        LocalDateTime updatedOn,
        String updatedBy,
        Instant timestamp
) {

    private static final int MAX_PAYLOAD_LENGTH = 1000;

    @JsonCreator
    public IncidentEvent {
        if (isBlank(id)) throw new IllegalArgumentException("id is required");
        if (isBlank(serviceName)) throw new IllegalArgumentException("serviceName is required");
        if (isBlank(exceptionType)) throw new IllegalArgumentException("exceptionType is required");
        if (category == null) throw new IllegalArgumentException("category is required");
        if (severity == null) throw new IllegalArgumentException("severity is required");
        if (metadata != null && metadata.length() > MAX_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException("metadata max length is 1000");
        }
        if (timestamp == null) timestamp = Instant.now();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

