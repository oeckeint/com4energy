package com.com4energy.processor.repository;

import java.time.LocalDateTime;

/**
 * Proyección para el pre-cargado del upsert: identifica una medida ya existente por su
 * business key (clienteId, fecha) y trae su {@code payloadHash} (para decidir omitir/actualizar)
 * y su {@code id} (para el UPDATE in-place).
 */
public interface ExistingMeasureView {

    Integer getClienteId();

    LocalDateTime getFecha();

    byte[] getPayloadHash();

    Long getId();
}
