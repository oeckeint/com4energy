package com.com4energy.recordsapi.controller.medidas.h.dto;

import java.time.LocalDateTime;

/**
 * Proyeccion de cantidad de registros por origen para una celda (cliente/hora/dia).
 * El "origen" deriva del file_record apuntado por medida_h.id_file_record
 * (nombre del archivo de carga), ya no de la antigua columna varchar "origen".
 */
public interface MedidaHCellOriginCount {
    String getOrigen();
    Long getRegistros();
    LocalDateTime getFechaCreacion();
}
