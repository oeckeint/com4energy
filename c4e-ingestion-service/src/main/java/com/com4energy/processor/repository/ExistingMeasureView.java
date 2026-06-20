package com.com4energy.processor.repository;

import java.time.LocalDateTime;

/**
 * Proyección para el pre-cargado del upsert: identifica una medida ya existente por su
 * business key (clienteId, fecha) y trae su {@code payloadHash} (para decidir omitir/actualizar),
 * su {@code id} (para el UPDATE in-place) y su procedencia
 * ({@code sourceFamilyKey}, {@code revision}, {@code processingIteration}) derivada del
 * {@code file_records} que la originó, para resolver la precedencia revisión/iteración POR FILA
 * y detectar colisiones cross-familia.
 */
public interface ExistingMeasureView {

    Integer getClienteId();

    LocalDateTime getFecha();

    byte[] getPayloadHash();

    Long getId();

    String getSourceFamilyKey();

    Integer getRevision();

    Integer getProcessingIteration();
}
