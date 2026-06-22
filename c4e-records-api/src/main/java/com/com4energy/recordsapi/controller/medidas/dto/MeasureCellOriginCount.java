package com.com4energy.recordsapi.controller.medidas.dto;

import java.time.LocalDateTime;

/**
 * Proyección de cantidad de registros por origen para una celda de medida
 * (cliente / hora [/ minuto] / día). Compartida por todos los tipos de medida (H, QH, …):
 * el "origen" deriva del file_record apuntado por {@code id_file_record} (nombre del archivo
 * de carga), idéntico entre tipos.
 */
public interface MeasureCellOriginCount {
    String getOrigen();
    Long getRegistros();
    LocalDateTime getFechaCreacion();
}
