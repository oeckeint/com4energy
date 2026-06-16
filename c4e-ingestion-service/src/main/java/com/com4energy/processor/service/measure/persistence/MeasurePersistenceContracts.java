package com.com4energy.processor.service.measure.persistence;

import com.com4energy.processor.service.measure.MeasureRecord;

import java.util.List;

/**
 * Contratos de corto plazo para persistir medidas sobre tablas existentes (sistemagestion).
 *
 * Esta capa desacopla el parser del modelo JPA concreto y permite evolucionar a un módulo compartido más adelante.
 */
public final class MeasurePersistenceContracts {

        private MeasurePersistenceContracts() {
        }

    public record PersistMeasuresCommand(
            Long fileRecordId,
            String origin,
            List<MeasureRecord> measureRecords,
            // true = la familia ya tiene un archivo aplicado (SUCCEEDED) -> los existentes que encuentre
            // el prefetch son de ESTA familia (revisión) -> upsert normal.
            // false = primera carga de la familia -> si el prefetch encuentra existentes, son de OTRA
            // familia -> colisión cross-familia -> se rechaza el archivo (pre-check, sin escribir).
            boolean familyHasPriorApplied
    ) {
        /** Conveniencia: por defecto asume historial previo (upsert normal, comportamiento seguro). */
        public PersistMeasuresCommand(Long fileRecordId, String origin, List<MeasureRecord> measureRecords) {
            this(fileRecordId, origin, measureRecords, true);
        }
    }

    public record MeasurePersistenceResult(
            int persistedCount,
            int updatedCount,
            int skippedCount,
            int errorCount,
            List<String> errors,
            List<MeasureRecord> failedRecords,
            String quarantineFilePath,
            // true = pre-check detectó que (cliente, fecha) ya pertenecen a otra familia -> el archivo
            // se rechazó SIN escribir nada.
            boolean crossFamilyCollision
    ) {
        public MeasurePersistenceResult {
            errors = List.copyOf(errors);
            failedRecords = failedRecords != null ? List.copyOf(failedRecords) : List.of();
        }

        /** Resultado normal (sin colisión cross-familia). */
        public MeasurePersistenceResult(
                int persistedCount,
                int updatedCount,
                int skippedCount,
                int errorCount,
                List<String> errors,
                List<MeasureRecord> failedRecords,
                String quarantineFilePath
        ) {
            this(persistedCount, updatedCount, skippedCount, errorCount, errors, failedRecords, quarantineFilePath, false);
        }

        /** Conveniencia (sin upsert): mantiene compatibilidad con llamadores previos. */
        public MeasurePersistenceResult(
                int persistedCount,
                int errorCount,
                int skippedCount,
                List<String> errors,
                List<MeasureRecord> failedRecords,
                String quarantineFilePath
        ) {
            this(persistedCount, 0, skippedCount, errorCount, errors, failedRecords, quarantineFilePath, false);
        }

        public MeasurePersistenceResult(
                int persistedCount,
                int errorCount,
                int skippedCount,
                List<String> errors
        ) {
            this(persistedCount, 0, skippedCount, errorCount, errors, List.of(), null, false);
        }

        /** Rechazo por colisión cross-familia (pre-check): cero escrituras. */
        public static MeasurePersistenceResult crossFamilyRejected() {
            return new MeasurePersistenceResult(0, 0, 0, 0, List.of(), List.of(), null, true);
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public boolean hasFailedRecords() {
            return !failedRecords.isEmpty();
        }
    }

    public interface MeasurePersistencePort {

        MeasurePersistenceResult persist(PersistMeasuresCommand command);
    }
}
