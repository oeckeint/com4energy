package com.com4energy.processor.service.processing;

import com.com4energy.persistence.filerecord.FileRecord;
import com.com4energy.persistence.filerecord.enums.FileType;

import java.nio.file.Path;
import java.util.Set;

public interface FileTypeProcessor {

    Set<FileType> supportedTypes();

    FileTypeProcessingResult process(FileRecord fileRecord, Path processingPath);
}

