package com.com4energy.recordsapi.messaging.filerecord;

import static com.com4energy.recordsapi.common.Constants.FILE_RECORD_LISTENER_ID_PREFIX;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.COMMENT;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.CREATED_BY;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.EVENT_TYPE;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.EXTENSION;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.FAILED_LINE_NUMBER;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.FAILED_LINE_REFERENCE;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.FILE_ID;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.FILENAME;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.FILE_RECORD_ID;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.FILE_TYPE;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.FINAL_PATH;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.ID;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.LINE;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.LINE_NUMBER;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.LINE_REFERENCE;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.METADATA;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.MEASURE_TYPE;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.OCCURRED_AT;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.ORIGINAL_FILENAME;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.ORIGIN;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.REASON;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.REASON_DESCRIPTION;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.REFERENCE;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.SOURCE_ID;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.STATUS;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.TYPE;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.UNKNOWN_FILE_RECORD_EVENT;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.USER;
import static com.com4energy.recordsapi.messaging.filerecord.FileRecordPayloadKeys.USERNAME;

import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;
import com.com4energy.recordsapi.domain.entity.messaging.FileRecordEvent;
import com.com4energy.recordsapi.repository.FileRecordEventRepository;
import com.com4energy.recordsapi.service.notifications.FileProcessingNotificationSseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registra dinámicamente un listener por cada tipo de file record event definido
 * en {@code c4e.file-records.types} del {@code application.yml}.
 *
 * <p>Maneja los eventos de archivo publicados por el ingestion-service
 * a través del outbox worker según la configuración activa.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileRecordConsumer implements RabbitListenerConfigurer {

    private static final String FILE_REGISTERED = "FILE_REGISTERED";
    private static final String FILE_MEASURE_PROCESSED = "FILE_MEASURE_PROCESSED";
    private static final String FILE_PROCESSING_PROCESSED = "FILE_PROCESSING_PROCESSED";
    private static final String FILE_PROCESSING_STARTED = "FILE_PROCESSING_STARTED";
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final DateTimeFormatter REGISTRATION_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FileRecordRoutingProperties props;
    private final FileRecordEventRepository repository;
    private final ObjectMapper objectMapper;
    private final FileProcessingNotificationSseService notificationSseService;

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        props.getTypes().forEach((typeKey, config) -> {
            SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
            endpoint.setId(FILE_RECORD_LISTENER_ID_PREFIX + typeKey);
            endpoint.setQueueNames(config.getQueue());
            endpoint.setMessageListener(message -> {
                try {
                    Map<String, Object> payload = objectMapper.readValue(
                            message.getBody(), new TypeReference<>() {});

                    String eventType = resolveEventType(typeKey, config, payload);
                    FileRecordEvent entity = mapToEntity(payload, eventType);
                    FileRecordEvent saved = repository.save(entity);
                    if (shouldPublishNotification(saved.getEventType())) {
                        notificationSseService.publish(saved);
                    }

                    log.info(Messages.format(RecordsApiCommonMessageKey.FILE_RECORD_EVENT_SAVED,
                            eventType, saved.getId(), saved.getFilename()));
                } catch (Exception e) {
                    log.error("Error processing file record event from queue={}: {}",
                            config.getQueue(), e.getMessage(), e);
                    throw new AmqpRejectAndDontRequeueException(
                            "Failed to process file record event from queue=" + config.getQueue(), e);
                }
            });

            registrar.registerEndpoint(endpoint);
            log.info("Registered file-record listener: id={} queue={}",
                    FILE_RECORD_LISTENER_ID_PREFIX + typeKey, config.getQueue());
        });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String resolveEventType(String typeKey, FileRecordRoutingProperties.QueueConfig config,
                                    Map<String, Object> payload) {
        String fromPayload = getString(payload, EVENT_TYPE);
        if (fromPayload != null && !fromPayload.isBlank()) {
            return fromPayload;
        }

        if (config.getEventType() != null && !config.getEventType().isBlank()) {
            return config.getEventType();
        }

        // Fallback para que typeKey en kebab/camel/snake termine en EVENT_TYPE estándar
        return normalizeToEventType(typeKey);
    }

    private FileRecordEvent mapToEntity(Map<String, Object> payload, String eventType) {
        String sourceId = resolveSourceId(payload);
        return FileRecordEvent.builder()
                .sourceId(sourceId)
                .filename(getStringWithFallback(payload, FILENAME, ORIGINAL_FILENAME))
                .extension(getString(payload, EXTENSION))
                .fileType(getStringWithFallback(payload, FILE_TYPE, MEASURE_TYPE, TYPE))
                .finalPath(getString(payload, FINAL_PATH))
                .status(resolveStatus(eventType, payload))
                .origin(getString(payload, ORIGIN))
                .failureReason(getString(payload, REASON))
                .failureReasonDescription(resolveFailureReasonDescription(eventType, payload, sourceId))
                .failedLineNumber(getIntegerWithFallback(payload, LINE_NUMBER, FAILED_LINE_NUMBER, LINE))
                .failedLineReference(getStringWithFallback(payload, LINE_REFERENCE, FAILED_LINE_REFERENCE, REFERENCE))
                .comment(getString(payload, COMMENT))
                .metadataJson(writeJson(payload.get(METADATA)))
                .rawPayload(writeJson(payload))
                .createdBy(getStringWithFallback(payload, CREATED_BY, USER, USERNAME))
                .eventType(eventType)
                .occurredAt(parseDateTime(getString(payload, OCCURRED_AT)))
                .build();
    }

    private String resolveFailureReasonDescription(String eventType, Map<String, Object> payload, String sourceId) {
        if (isFileRegisteredEvent(eventType)) {
            return buildInitialRegistrationDescription(parseDateTime(getString(payload, OCCURRED_AT)));
        }

        String fromPayload = getString(payload, REASON_DESCRIPTION);
        if (fromPayload != null && !fromPayload.isBlank()) {
            return fromPayload;
        }

        if (!isFileProcessingProcessedEvent(eventType)) {
            return null;
        }

        Map<String, Object> metadata = toMetadataMap(payload.get(METADATA));
        if (metadata.isEmpty()) {
            return null;
        }

        String filename = getStringWithFallback(payload, FILENAME, ORIGINAL_FILENAME);
        return "event=measure_file_processed"
                + " fileId=" + valueOrDash(sourceId)
                + " filename='" + valueOrDash(filename) + "'"
                + " measureType=" + valueOrDash(metadata.get(MEASURE_TYPE))
                + " total=" + valueOrDash(metadata.get("total"))
                + " persisted=" + valueOrDash(metadata.get("persisted"))
                + " defects=" + valueOrDash(metadata.get("defects"))
                + " skipped=" + valueOrDash(metadata.get("skipped"))
                + " targetTable=" + valueOrDash(metadata.get("targetTable"))
                + " totalMs=" + valueOrDash(metadata.get("totalMs"))
                + " parseMs=" + valueOrDash(metadata.get("parseMs"))
                + " persistMs=" + valueOrDash(metadata.get("persistMs"));
    }

    private String buildInitialRegistrationDescription(LocalDateTime uploadedAt) {
        if (uploadedAt == null) {
            return "Registrado en file_records (PENDING): -";
        }
        return "Registrado en file_records (PENDING): " + uploadedAt.format(REGISTRATION_TIMESTAMP_FORMAT);
    }

    private boolean shouldPublishNotification(String eventType) {
        // FILE_PROCESSING_STARTED no genera toast (es ruido interno de inicio de proceso)
        return !isFileProcessingStartedEvent(eventType);
    }

    private boolean isFileRegisteredEvent(String eventType) {
        return FILE_REGISTERED.equalsIgnoreCase(eventType);
    }

    private boolean isFileProcessingStartedEvent(String eventType) {
        return FILE_PROCESSING_STARTED.equalsIgnoreCase(eventType);
    }

    private boolean isFileProcessingProcessedEvent(String eventType) {
        return FILE_PROCESSING_PROCESSED.equalsIgnoreCase(eventType)
                || FILE_MEASURE_PROCESSED.equalsIgnoreCase(eventType);
    }

    private Map<String, Object> toMetadataMap(Object metadataRaw) {
        if (metadataRaw == null) {
            return Map.of();
        }
        if (metadataRaw instanceof Map<?, ?> metadataMap) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            metadataMap.forEach((key, value) -> normalized.put(String.valueOf(key), value));
            return normalized;
        }
        if (metadataRaw instanceof String metadataString) {
            if (metadataString.isBlank()) {
                return Map.of();
            }
            try {
                return objectMapper.readValue(metadataString, new TypeReference<>() {});
            } catch (Exception ignored) {
                return Map.of();
            }
        }
        return Map.of();
    }

    private String valueOrDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = value.toString();
        return text.isBlank() ? "-" : text;
    }

    private String resolveStatus(String eventType, Map<String, Object> payload) {
        String fromPayload = getString(payload, STATUS);
        if (fromPayload != null && !fromPayload.isBlank()) {
            return fromPayload;
        }
        if (isFileProcessingProcessedEvent(eventType)) {
            return STATUS_SUCCEEDED;
        }
        return "UNKNOWN";
    }

    /** Soporta shape histórica (id/fileRecordId) y shape de rechazo (fileId). */
    private String resolveSourceId(Map<String, Object> payload) {
        String fromSourceId = getString(payload, SOURCE_ID);
        if (fromSourceId != null && !fromSourceId.isBlank()) {
            return fromSourceId;
        }
        String fromFileId = getString(payload, FILE_ID);
        if (fromFileId != null && !fromFileId.isBlank()) {
            return fromFileId;
        }
        String id = getString(payload, FILE_RECORD_ID);
        return (id != null) ? id : getString(payload, ID);
    }

    private String normalizeToEventType(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN_FILE_RECORD_EVENT;
        }
        return raw
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private String getString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return (value != null) ? value.toString() : null;
    }

    private String getStringWithFallback(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            String value = getString(payload, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Integer getIntegerWithFallback(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ex) {
                log.warn("Could not parse {}='{}' as Integer", key, value);
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        }
        catch (Exception ignored) {
            // Try with timezone/offset, e.g. 2026-04-28T06:30:00Z
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Could not parse occurredAt value='{}', defaulting to null", value);
            return null;
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Could not serialize payload fragment to JSON: {}", e.getMessage());
            return value.toString();
        }
    }
}

