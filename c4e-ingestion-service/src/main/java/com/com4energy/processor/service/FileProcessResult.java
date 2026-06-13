package com.com4energy.processor.service;

import com.com4energy.persistence.filerecord.FileRecord;
import lombok.Builder;

@Builder
record FileProcessResult(Status status, FileRecord fileRecord) {
    enum Status {
        UPLOADED,
        ERROR
    }
}

