package com.com4energy.recordsapi.domain.entity.messaging;

import com.com4energy.event.publisher.common.Environment;
import com.com4energy.event.publisher.incident.contract.IncidentCategory;
import com.com4energy.event.publisher.incident.contract.IncidentSeverity;
import com.com4energy.event.publisher.incident.contract.IncidentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "incidents", schema = "sge")

@NoArgsConstructor @AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment;

    private String endpoint;

    @Column(name = "method_name")
    private String methodName;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "exception_type", nullable = false)
    private String exceptionType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentSeverity severity;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentStatus status = IncidentStatus.NEW;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "filename")
    private String filename;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "folder_name")
    private String folderName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private JsonNode metadata;

    @Column(name = "created_on", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdOn;

    private String createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    private String updatedBy;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

}
