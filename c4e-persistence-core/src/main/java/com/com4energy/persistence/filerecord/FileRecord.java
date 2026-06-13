package com.com4energy.persistence.filerecord;

import java.time.LocalDateTime;
import java.util.Optional;

import com.com4energy.persistence.audit.Auditable;
import com.com4energy.persistence.filerecord.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.io.FilenameUtils;

@Entity
@Table(name = "file_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FileRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFilename;
    private String finalFilename;
    private String finalPath;

    private String extension;

    @Enumerated(EnumType.STRING)
    private FileType type;
    private String comment;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @Enumerated(EnumType.STRING)
    private QualityStatus qualityStatus;

    @Enumerated(EnumType.STRING)
    private BusinessResult businessResult;

    @Enumerated(EnumType.STRING)
    private FileOrigin origin;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean locked;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private Integer retryCount;
    private Integer processedRecords;
    private Integer defectedRecords;
    private Long parseDurationMs;
    private Long processingDurationMs;
    private String hash;

    @Embedded
    private MeasureFileVersion measureVersion;

    @Enumerated(EnumType.STRING)
    private FailureReason failureReason;

    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private LocalDateTime failedAt;
    private LocalDateTime lastAttemptAt;

    public String getExtension() {
        return FilenameUtils.getExtension(Optional.ofNullable(this.originalFilename).orElse(this.extension));
    }

    public boolean hasMeasureVersion() {
        return measureVersion != null;
    }

    public void markAsFailed() {
        this.status = FileStatus.FAILED;
        this.failedAt = LocalDateTime.now();
    }

    public void markAsFailedWithComment(String comment) {
        this.status = FileStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.comment = comment;
    }

    public void markAsNew() {
        this.status = FileStatus.NEW;
        this.uploadedAt = LocalDateTime.now();
    }

    public void markAsPending() {
        this.status = FileStatus.PENDING;
        this.uploadedAt = LocalDateTime.now();
    }

    public void markAsProcessing() {
        this.status = FileStatus.PROCESSING;
        this.lastAttemptAt = LocalDateTime.now();
    }

    public void markAsRejected() {
        this.status = FileStatus.REJECTED;
    }

    public void markAsDuplicatedOriginalFilename() {
        this.status = FileStatus.DUPLICATED_ORIGINAL_FILENAME;
    }

    public void markAsDuplicatedContent() {
        this.status = FileStatus.DUPLICATED_CONTENT;
    }

    public void markAsRetry(FailureReason reason) {
        this.failureReason = reason;
        this.status = FileStatus.RETRY;
        this.lastAttemptAt = LocalDateTime.now();
        this.retryCount = Optional.ofNullable(this.retryCount).orElse(0) + 1;
    }

    public void markAsSucceded() {
        this.status = FileStatus.SUCCEEDED;
        this.processedAt = LocalDateTime.now();
    }

}
