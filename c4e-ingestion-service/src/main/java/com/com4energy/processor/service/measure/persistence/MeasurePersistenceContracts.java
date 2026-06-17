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
            // Procedencia del archivo entrante: familia + (revisión, iteración). El adapter resuelve
            // la precedencia POR FILA contra lo existente (no rechaza por antigüedad a nivel archivo)
            // y detecta colisiones cross-familia comparando la familia real de cada fila existente.
            String sourceFamilyKey,
            Integer revision,
            Integer processingIteration
    ) {
        /**
         * Conveniencia sin procedencia (familia null): deshabilita la detección cross-familia y trata
         * lo existente como de la misma familia. Para llamadores/tests que no modelan la versión.
         */
        public PersistMeasuresCommand(Long fileRecordId, String origin, List<MeasureRecord> measureRecords) {
            this(fileRecordId, origin, measureRecords, null, null, null);
        }
    }

    public record MeasurePersistenceResult(
            int persistedCount,
            int updatedCount,
            // Omitidas porque el contenido era idéntico (mismo payload_hash) -> no había nada que escribir.
            int skippedIdenticalCount,
            // Omitidas porque ya existía una revisión/iteración igual o más reciente para esa
            // (cliente, fecha) -> el dato entrante es obsoleto.
            int skippedStaleCount,
            int errorCount,
            List<String> errors,
            List<MeasureRecord> failedRecords,
            String quarantineFilePath,
            // true = se detectó que (cliente, fecha) ya pertenecen a otra familia -> el archivo
            // se rechazó SIN escribir nada.
            boolean crossFamilyCollision
    ) {
        public MeasurePersistenceResult {
            errors = List.copyOf(errors);
            failedRecords = failedRecords != null ? List.copyOf(failedRecords) : List.of();
        }

        /** Resultado normal (sin colisión cross-familia), con los dos tipos de omisión separados. */
        public MeasurePersistenceResult(
                int persistedCount,
                int updatedCount,
                int skippedIdenticalCount,
                int skippedStaleCount,
                int errorCount,
                List<String> errors,
                List<MeasureRecord> failedRecords,
                String quarantineFilePath
        ) {
            this(persistedCount, updatedCount, skippedIdenticalCount, skippedStaleCount,
                    errorCount, errors, failedRecords, quarantineFilePath, false);
        }

        /** Conveniencia legacy: una sola cuenta de "skipped" se interpreta como omisión por idéntico. */
        public MeasurePersistenceResult(
                int persistedCount,
                int updatedCount,
                int skippedCount,
                int errorCount,
                List<String> errors,
                List<MeasureRecord> failedRecords,
                String quarantineFilePath
        ) {
            this(persistedCount, updatedCount, skippedCount, 0, errorCount, errors, failedRecords, quarantineFilePath, false);
        }

        /** Conveniencia legacy (sin updated). */
        public MeasurePersistenceResult(
                int persistedCount,
                int errorCount,
                int skippedCount,
                List<String> errors,
                List<MeasureRecord> failedRecords,
                String quarantineFilePath
        ) {
            this(persistedCount, 0, skippedCount, 0, errorCount, errors, failedRecords, quarantineFilePath, false);
        }

        /** Conveniencia legacy mínima. */
        public MeasurePersistenceResult(
                int persistedCount,
                int errorCount,
                int skippedCount,
                List<String> errors
        ) {
            this(persistedCount, 0, skippedCount, 0, errorCount, errors, List.of(), null, false);
        }

        /** Rechazo por colisión cross-familia: cero escrituras. */
        public static MeasurePersistenceResult crossFamilyRejected() {
            return new MeasurePersistenceResult(0, 0, 0, 0, 0, List.of(), List.of(), null, true);
        }

        /** Total de omitidas (idénticas + obsoletas). */
        public int skippedCount() {
            return skippedIdenticalCount + skippedStaleCount;
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
