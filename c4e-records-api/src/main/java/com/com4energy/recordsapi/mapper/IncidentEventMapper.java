package com.com4energy.recordsapi.mapper;

import com.com4energy.recordsapi.domain.entity.messaging.Incident;
import com.com4energy.event.publisher.incident.contract.IncidentStatus;
import com.com4energy.event.publisher.incident.contract.IncidentEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Convierte un {@link IncidentEvent} recibido por RabbitMQ en una entidad {@link Incident}
 * lista para ser persistida.
 *
 * <p>El campo {@code payload} se parsea como {@link JsonNode}; si no es JSON valido
 * se almacena como nodo de texto para no perder la informacion.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentEventMapper {

    private final ObjectMapper objectMapper;

    public Incident toIncident(IncidentEvent event) {
        JsonNode metadata = parsePayload(event.payload());

        return Incident.builder()
                .serviceName(event.serviceName())
                .environment(event.environment())
                .endpoint(event.endpoint())
                .methodName(event.methodName())
                .httpMethod(event.httpMethod())
                .traceId(event.traceId())
                .spanId(event.spanId())
                .userId(event.userId())
                .exceptionType(event.exceptionType())
                .message(event.message())
                .stackTrace(event.stackTrace())
                .category(event.category())
                .severity(event.severity())
                .status(event.status() != null ? event.status() : IncidentStatus.NEW)
                .errorCode(event.errorCode())
                .filename(event.filename())
                .fileType(event.fileType())
                .folderName(event.folderName())
                .createdBy(event.createdBy())
                .updatedOn(event.updatedOn())
                .updatedBy(event.updatedBy())
                .metadata(metadata)
                .build();
    }


    private JsonNode parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("Payload is not valid JSON, storing as plain text: {}", e.getMessage());
            return objectMapper.getNodeFactory().textNode(payload);
        }
    }
}

