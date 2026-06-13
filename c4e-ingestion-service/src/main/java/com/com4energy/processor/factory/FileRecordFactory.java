package com.com4energy.processor.factory;

import java.util.Locale;
import java.util.Optional;

import com.com4energy.persistence.filerecord.enums.FileOrigin;
import com.com4energy.persistence.filerecord.FileRecord;
import com.com4energy.persistence.filerecord.enums.FileType;
import com.com4energy.persistence.filerecord.MeasureFileVersion;
import com.com4energy.persistence.filerecord.enums.QualityStatus;
import com.com4energy.persistence.filerecord.enums.BusinessResult;
import com.com4energy.processor.service.dto.FileContext;
import com.com4energy.processor.util.FileNameVersionParserUtil;
import org.apache.commons.io.FilenameUtils;

public final class FileRecordFactory {

    private FileRecordFactory() {
    }

    public static FileRecord from(FileContext fileContext) {
        return from(fileContext, FileOrigin.API);
    }

    public static FileRecord from(FileContext fileContext, FileOrigin origin) {
        String finalFilename = Optional.of(fileContext.findStoredFilePath().orElse(""))
                .map(FilenameUtils::getName)
                .filter(name -> !name.isBlank())
                .orElseGet(() -> Optional.ofNullable(fileContext.validationContext().getOriginalFilename())
                        .filter(name -> !name.isBlank())
                        .orElseThrow(() -> new IllegalStateException(
                                "Cannot determine finalFilename for FileRecord.from; originalFilename and storedPath are empty"
                        )));

        String originalFilename = fileContext.validationContext().getOriginalFilename();
        FileNameVersionParserUtil.Result version = FileNameVersionParserUtil.parse(originalFilename);

        return FileRecord.builder()
                .originalFilename(originalFilename)
                .finalFilename(finalFilename)
                .finalPath(fileContext.findStoredFilePath().orElse(null))
                .type(determineInitialType(originalFilename))
                .hash(fileContext.validationContext().getOrComputeHash())
                .origin(origin)
                .qualityStatus(QualityStatus.NOT_EVALUATED)
                .businessResult(BusinessResult.NOT_PROCESSED)
                .retryCount(0)
                .measureVersion(toMeasureFileVersion(version))
                .build();
    }

    public static MeasureFileVersion toMeasureFileVersion(FileNameVersionParserUtil.Result result) {
        if (result.sourceFamilyKey() == null) {
            return null;
        }
        return new MeasureFileVersion(
                result.sourceFamilyKey(),
                result.revision(),
                result.processingIteration()
        );
    }

    private static FileType determineInitialType(String originalFilename) {
        String extension = Optional.ofNullable(FilenameUtils.getExtension(originalFilename))
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .orElse("");

        if ("xml".equals(extension)) {
            return FileType.AWAITING_CLASSIFICATION;
        }

        if (extension.length() == 1 && Character.isDigit(extension.charAt(0))) {
            return resolveMeasureTypeFromFilename(originalFilename);
        }

        return FileType.UNKNOWN;
    }

    private static FileType resolveMeasureTypeFromFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return FileType.UNKNOWN;
        }

        String baseName = FilenameUtils.getBaseName(originalFilename);
        String[] tokens = baseName.split("_");
        if (tokens.length == 0 || tokens[0].length() < 2) {
            return FileType.UNKNOWN;
        }

        String prefix = tokens[0].substring(0, 2).toLowerCase(Locale.ROOT);
        return switch (prefix) {
            case "p1" -> FileType.MEDIDA_H_P1;
            case "p2" -> FileType.MEDIDA_QH_P2;
            case "f5" -> FileType.MEDIDA_CCH_F5;
            default -> FileType.UNKNOWN;
        };
    }

}
