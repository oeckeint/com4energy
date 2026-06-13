package com.com4energy.processor.api.response;

import com.com4energy.persistence.filerecord.enums.FileStatus;
import lombok.Builder;

@Builder
public record FileMetadata(
        String originalFilename,
        FileStatus status,
        String hash) {}
