package com.com4energy.processor.service.processing;

import com.com4energy.i18n.core.Messages;
import com.com4energy.processor.common.LogsCommonMessageKey;
import com.com4energy.persistence.filerecord.enums.FailureReason;
import com.com4energy.persistence.filerecord.FileRecord;
import com.com4energy.persistence.filerecord.enums.FileType;
import com.com4energy.persistence.filerecord.enums.BusinessResult;
import com.com4energy.persistence.filerecord.enums.QualityStatus;
import com.com4energy.processor.service.measure.MeasureFileParserService;
import com.com4energy.processor.service.measure.MeasureParseResult;
import com.com4energy.processor.service.measure.MeasureRevisionGuard;
import com.com4energy.processor.service.measure.persistence.MeasurePersistenceContracts;
import com.com4energy.processor.service.measure.validation.MeasureDefectReportService;
import com.com4energy.processor.service.measure.validation.MeasureRecordValidationChain;
import com.com4energy.processor.service.measure.validation.MeasureRecordValidationResult;
import com.com4energy.processor.service.measure.validation.ValidationMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class MeasureFileTypeProcessor implements FileTypeProcessor {

    private static final Set<FileType> SUPPORTED_TYPES = Set.of(
            FileType.MEDIDA_H_P1,
            FileType.MEDIDA_QH_P2,
            FileType.MEDIDA_CCH_F5
    );

    private final MeasureFileParserService measureFileParserService;
    private final MeasurePersistenceContracts.MeasurePersistencePort measurePersistencePort;
    private final MeasureRecordValidationChain measureRecordValidationChain;
    private final MeasureDefectReportService measureDefectReportService;
    private final MeasureRevisionGuard measureRevisionGuard;

    public MeasureFileTypeProcessor(
            MeasureFileParserService measureFileParserService,
            MeasurePersistenceContracts.MeasurePersistencePort measurePersistencePort,
            MeasureRecordValidationChain measureRecordValidationChain,
            MeasureDefectReportService measureDefectReportService,
            MeasureRevisionGuard measureRevisionGuard
    ) {
        this.measureFileParserService = measureFileParserService;
        this.measurePersistencePort = measurePersistencePort;
        this.measureRecordValidationChain = measureRecordValidationChain;
        this.measureDefectReportService = measureDefectReportService;
        this.measureRevisionGuard = measureRevisionGuard;
    }

    @Override
    public Set<FileType> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public FileTypeProcessingResult process(FileRecord fileRecord, Path processingPath) {
        long parseStartedAtNanos = System.nanoTime();

        // Guard de precedencia (autoritativo): si ya se aplicó una revisión igual o más reciente
        // de esta familia, descartar antes del upsert para no pisar datos más nuevos.
        if (measureRevisionGuard.isSupersededByApplied(fileRecord.getMeasureVersion())) {
            String comment = "Revisión superseded: ya se aplicó una versión igual o más reciente de la familia";
            log.info("Skipping file '{}' (id={}): {}", fileRecord.getOriginalFilename(), fileRecord.getId(), comment);
            fileRecord.setBusinessResult(BusinessResult.NOT_PERSISTED);
            // Reporta el rechazo al outbox worker (FILE_REJECTED) además de marcarlo en BD.
            return FileTypeProcessingResult.rejected(
                    FailureReason.SUPERSEDED_REVISION,
                    comment,
                    List.of(FileTypeProcessingResult.DeferredOutboxEvent.fileRejected())
            );
        }

        try {
            // Use the FileType from database (already classified at file ingestion)
            // Don't recalculate it - the job just reads and processes based on pre-classified type
            MeasureParseResult result = measureFileParserService.parse(processingPath, fileRecord.getType());
            long parseDurationMs = elapsedMillis(parseStartedAtNanos);
            fileRecord.setParseDurationMs(parseDurationMs);

            // Si NINGÚN registro parseó, el archivo es inservible -> fallo total (con el reporte de
            // defectos de parseo si los hubo).
            if (result.records().isEmpty()) {
                fileRecord.setProcessedRecords(0);
                fileRecord.setDefectedRecords(result.errorCount());
                fileRecord.setQualityStatus(result.hasErrors() ? QualityStatus.WITH_DEFECTS : QualityStatus.NOT_EVALUATED);
                fileRecord.setBusinessResult(BusinessResult.NOT_PERSISTED);
                if (result.hasErrors()) {
                    Optional<Path> parseDefectPath = measureDefectReportService.writeParseDefectReport(
                            fileRecord.getOriginalFilename(), result.errors());
                    return FileTypeProcessingResult.failed(
                            FailureReason.INVALID_FILE_FORMAT,
                            buildDefectComment(result.errorCount(), parseDefectPath),
                            buildDefectReportDeferredEvents("parse", result.errorCount(), parseDefectPath));
                }
                return FileTypeProcessingResult.failed(
                        FailureReason.INVALID_FILE_FORMAT,
                        "El archivo no contiene registros de medidas procesables");
            }

            // Parseo TOLERANTE: las líneas con error de parseo se reportan como defecto y se continúa
            // persistiendo las que sí parsearon (antes: una línea mala rechazaba el archivo completo).
            List<FileTypeProcessingResult.DeferredOutboxEvent> deferredOutboxEvents = new ArrayList<>();
            int parseErrorCount = result.errorCount();
            if (result.hasErrors()) {
                Optional<Path> parseDefectPath = measureDefectReportService.writeParseDefectReport(
                        fileRecord.getOriginalFilename(), result.errors());
                deferredOutboxEvents.addAll(buildDefectReportDeferredEvents("parse", parseErrorCount, parseDefectPath));
            }

            MeasureRecordValidationResult validationResult = measureRecordValidationChain.validate(
                    result.records(),
                    ValidationMode.TOLERANT
            );
            int validationErrorCount = validationResult.errorCount();

            // Indica si la familia ya tiene un archivo aplicado. Si NO, y el prefetch del adapter
            // encuentra (cliente, fecha) existentes, son de otra familia -> colisión cross-familia.
            boolean familyHasPriorMeasures = measureRevisionGuard.hasAppliedSibling(fileRecord.getMeasureVersion());

            long persistStartedAtNanos = System.nanoTime();
            MeasurePersistenceContracts.MeasurePersistenceResult persistenceResult = measurePersistencePort.persist(
                    new MeasurePersistenceContracts.PersistMeasuresCommand(
                            fileRecord.getId(),
                            fileRecord.getOriginalFilename(),
                            validationResult.validRecords(),
                            familyHasPriorMeasures
                    )
            );
            long persistDurationMs = elapsedMillis(persistStartedAtNanos);

            // Colisión cross-familia detectada en el pre-check (no se escribió nada) -> rechazar.
            if (persistenceResult.crossFamilyCollision()) {
                String comment = "Colisión cross-familia: (cliente, fecha) ya pertenecen a otra familia";
                log.warn("Rechazando archivo '{}' (id={}): {}",
                        fileRecord.getOriginalFilename(), fileRecord.getId(), comment);
                fileRecord.setBusinessResult(BusinessResult.NOT_PERSISTED);
                return FileTypeProcessingResult.rejected(
                        FailureReason.CROSS_FAMILY_COLLISION,
                        comment,
                        List.of(FileTypeProcessingResult.DeferredOutboxEvent.fileRejected())
                );
            }

            // total = TODAS las filas de medida que llegaron en el archivo (parsearon OK + fallaron al
            // parsear). Así una sola invariante reconcilia el archivo completo:
            //   total = persisted + updated + skipped + parseDefects + validationDefects + quarantined
            int totalMeasuresInFile = result.successCount() + parseErrorCount;
            int persistedMeasures = persistenceResult.persistedCount();
            int updatedMeasures = persistenceResult.updatedCount();
            // Buckets de defecto desglosados por capa (suman el agregado defectCount):
            int parseDefects = parseErrorCount;                                       // no parsearon
            int validationDefects = validationErrorCount + persistenceResult.errorCount(); // rechazados antes del INSERT
            int quarantinedMeasures = persistenceResult.failedRecords().size();       // aislados por binary-split (constraint BD)
            int defectCount = parseDefects + validationDefects + quarantinedMeasures;
            int skippedMeasures = persistenceResult.skippedCount();
            // Manejados OK = insertados + actualizados (revisión) + omitidos (idénticos por hash).
            int handledMeasures = persistedMeasures + updatedMeasures + skippedMeasures;
            boolean hasValidationOrPersistenceErrors = validationResult.hasErrors() || persistenceResult.hasErrors();
            boolean hasQuarantineRecords = persistenceResult.hasFailedRecords();
            boolean hasDefects = parseErrorCount > 0 || hasValidationOrPersistenceErrors || hasQuarantineRecords;
            long totalProcessingMs = elapsedMillis(parseStartedAtNanos);
            FileType resolvedMeasureType = fileRecord.getType() != null
                    ? fileRecord.getType()
                    : result.metadata().kind();
            String measureType = resolvedMeasureType.name();
            String destinationStore = resolveDestinationStore(resolvedMeasureType);

            fileRecord.setProcessedRecords(handledMeasures);
            fileRecord.setDefectedRecords(defectCount);
            fileRecord.setQualityStatus(hasDefects ? QualityStatus.WITH_DEFECTS : QualityStatus.CLEAN);
            if (!hasDefects) {
                // Incluye el reproceso idéntico (todo omitido): sin defectos = éxito.
                fileRecord.setBusinessResult(BusinessResult.FULLY_SUCCEEDED);
            } else if (handledMeasures > 0) {
                fileRecord.setBusinessResult(BusinessResult.PARTIAL_SUCCEEDED);
            } else {
                fileRecord.setBusinessResult(BusinessResult.NOT_PERSISTED);
            }


            log.info(Messages.format(
                    LogsCommonMessageKey.MEASURE_FILE_PROCESSED,
                    fileRecord.getId(),
                    fileRecord.getOriginalFilename(),
                    measureType,
                    totalMeasuresInFile,
                    persistedMeasures,
                    defectCount,
                    skippedMeasures,
                    destinationStore,
                    totalProcessingMs,
                    parseDurationMs,
                    persistDurationMs,
                    updatedMeasures,
                    parseDefects,
                    validationDefects,
                    quarantinedMeasures
            ));

            if (!hasDefects) {
                String processedMetadataJson = "{"
                        + "\"measureType\":\"" + measureType + "\","
                        + "\"targetTable\":\"" + destinationStore + "\","
                        + "\"total\":" + totalMeasuresInFile + ","
                        + "\"persisted\":" + persistedMeasures + ","
                        + "\"updated\":" + updatedMeasures + ","
                        + "\"skipped\":" + skippedMeasures + ","
                        + "\"defects\":" + defectCount + ","
                        + "\"parseDefects\":" + parseDefects + ","
                        + "\"validationDefects\":" + validationDefects + ","
                        + "\"quarantined\":" + quarantinedMeasures + ","
                        + "\"totalMs\":" + totalProcessingMs + ","
                        + "\"parseMs\":" + parseDurationMs + ","
                        + "\"persistMs\":" + persistDurationMs
                        + "}";
                deferredOutboxEvents.add(FileTypeProcessingResult.DeferredOutboxEvent.measureProcessed(processedMetadataJson));
            }

            // Cuarentena: registros aislados por binary split — flujo EXCEPCIONAL
            // Se reportan por separado (.sge_quarantine.jsonl) y se publican al outbox
            if (persistenceResult.hasFailedRecords()) {
                buildQuarantineDeferredEvent(fileRecord, persistenceResult).ifPresent(deferredOutboxEvents::add);
            }

            Optional<FileTypeProcessingResult> defectResult = handleValidationAndPersistenceErrors(
                    fileRecord,
                    validationResult,
                    persistenceResult,
                    hasValidationOrPersistenceErrors,
                    handledMeasures,
                    deferredOutboxEvents
            );
            if (defectResult.isPresent()) {
                return defectResult.get();
            }

            // Solo es fallo total si NADA se manejó OK (ni insert, ni update, ni skip) y hubo cuarentena.
            // Con upsert, un archivo de corrección puede tener 0 inserts pero sí updates/skips válidos.
            if (hasQuarantineRecords && handledMeasures == 0) {
                String comment = "No se pudo persistir ninguna medida; registros aislados en cuarentena por persistencia.";
                fileRecord.setComment(comment);
                return FileTypeProcessingResult.failed(FailureReason.INVALID_FILE_FORMAT, comment, deferredOutboxEvents);
            }

            return FileTypeProcessingResult.success(deferredOutboxEvents);
        } catch (RuntimeException | java.io.IOException ex) {
            fileRecord.setParseDurationMs(elapsedMillis(parseStartedAtNanos));
            throw new IllegalStateException("Error processing measure file '" + fileRecord.getOriginalFilename() + "'", ex);
        }
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private String resolveDestinationStore(FileType measureType) {
        if (measureType == null) {
            return "desconocido";
        }

        return switch (measureType) {
            case MEDIDA_H_P1 -> "medida_h";
            case MEDIDA_QH_P2 -> "medida_qh";
            case MEDIDA_CCH_F5 -> "medida_cch";
            default -> "desconocido";
        };
    }

    private String buildDefectComment(int incidentCount, Optional<Path> reportPath) {
        String filename = reportPath.map(p -> p.getFileName().toString()).orElse("(reporte no generado)");
        return "Se generó " + filename + " con " + incidentCount + " incidencia(s)";
    }

    private List<FileTypeProcessingResult.DeferredOutboxEvent> buildDefectReportDeferredEvents(
            String phase,
            int incidentCount,
            Optional<Path> defectPath
    ) {
        return defectPath
                .map(path -> List.of(FileTypeProcessingResult.DeferredOutboxEvent.defectReportCreated(phase, incidentCount, path)))
                .orElseGet(List::of);
    }

    /**
     * Maneja registros aislados por binary split.
     * Flujo excepcional: el registro pasó validación pero falló en BD (constraint, FK, etc.).
     * → Escribe .sge_quarantine.jsonl (SEPARADO del .sge_defect.jsonl de validación)
     * → Publica FILE_PERSISTENCE_QUARANTINE al outbox para auditoría y posible retry
     */
    private Optional<FileTypeProcessingResult.DeferredOutboxEvent> buildQuarantineDeferredEvent(
            FileRecord fileRecord,
            MeasurePersistenceContracts.MeasurePersistenceResult persistenceResult
    ) {
        int failedCount = persistenceResult.failedRecords().size();
        log.warn(
                "Binary split quarantine: {} record(s) isolated for file '{}'. Publishing quarantine event.",
                failedCount,
                fileRecord.getOriginalFilename()
        );

        List<MeasureDefectReportService.PersistenceFailedRecord> quarantineEntries =
                persistenceResult.failedRecords().stream()
                        .map(failedRecord -> new MeasureDefectReportService.PersistenceFailedRecord(
                                failedRecord,
                                failedRecord.kind().name(),
                                "Binary split isolation — record could not be persisted",
                                failedRecord.cups() + "@" + failedRecord.timestamp()
                        ))
                        .toList();

        Optional<Path> quarantinePath = measureDefectReportService.writeQuarantineDefectReport(
                fileRecord.getOriginalFilename(),
                quarantineEntries
        );

        log.info(
                "Quarantine report prepared for deferred outbox publish: fileId={}, failedRecords={}, path={}",
                fileRecord.getId(),
                failedCount,
                quarantinePath.map(p -> p.toAbsolutePath().toString()).orElse("N/A")
        );

        return quarantinePath.map(path -> FileTypeProcessingResult.DeferredOutboxEvent.persistenceQuarantine(failedCount, path));
    }

    private Optional<FileTypeProcessingResult> handleValidationAndPersistenceErrors(
            FileRecord fileRecord,
            MeasureRecordValidationResult validationResult,
            MeasurePersistenceContracts.MeasurePersistenceResult persistenceResult,
            boolean hasValidationOrPersistenceErrors,
            int handledMeasures,
            List<FileTypeProcessingResult.DeferredOutboxEvent> deferredOutboxEvents
    ) {
        if (!hasValidationOrPersistenceErrors) {
            return Optional.empty();
        }

        int totalIncidents = validationResult.errorCount() + persistenceResult.errorCount();
        Optional<Path> defectPath = measureDefectReportService.writeValidationAndPersistenceDefectReport(
                fileRecord.getOriginalFilename(),
                validationResult.errors(),
                persistenceResult.errors()
        );
        String comment = buildDefectComment(totalIncidents, defectPath);
        deferredOutboxEvents.addAll(buildDefectReportDeferredEvents("validation", totalIncidents, defectPath));

        // Éxito parcial si algo se manejó OK (insert/update/skip), aunque haya defectos.
        if (handledMeasures > 0) {
            fileRecord.setComment(comment);
            return Optional.of(FileTypeProcessingResult.success(deferredOutboxEvents));
        }

        return Optional.of(FileTypeProcessingResult.failed(FailureReason.INVALID_FILE_FORMAT, comment, deferredOutboxEvents));
    }

}
