package com.com4energy.recordsapi.domain.entity.messaging;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro de eventos de archivos recibidos desde el ingestion-service
 * a través del outbox worker.
 *
 * <p>Almacena eventos de archivo publicados por otros servicios, por ejemplo
 * {@code FILE_REJECTED} y otros tipos futuros definidos por configuración.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "file_record_events")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRecordEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID del registro en el ingestion-service (id / fileRecordId / fileId según payload). */
    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "final_path", length = 500)
    private String finalPath;

    /** PENDING, REJECTED, etc. */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** API, AUTOMATIC, etc. */
    @Column(name = "origin", length = 30)
    private String origin;

    @Column(name = "failure_reason", length = 50)
    private String failureReason;

    @Column(name = "failure_reason_description", columnDefinition = "TEXT")
    private String failureReasonDescription;

    @Column(name = "failed_line_number")
    private Integer failedLineNumber;

    @Column(name = "failed_line_reference", columnDefinition = "TEXT")
    private String failedLineReference;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    /** Tipo de evento tal como viene del outbox, por ejemplo FILE_REJECTED. */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Momento en que ocurrió el evento en el ingestion-service. */
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    /** Momento en que records-api recibió el mensaje. */
    @Column(name = "received_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime receivedAt;
}

