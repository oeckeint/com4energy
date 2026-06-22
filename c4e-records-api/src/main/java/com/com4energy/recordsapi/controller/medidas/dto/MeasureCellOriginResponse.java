package com.com4energy.recordsapi.controller.medidas.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle de orígenes (archivos de carga) que aportan filas a una celda de medida.
 * Compartido por todos los tipos: {@code minuto} es null para Medida H (granularidad
 * horaria) y se informa para QH (cuarto de hora).
 */
public record MeasureCellOriginResponse(
        Integer clienteId,
        Integer hora,
        Integer minuto,
        Long totalRegistros,
        Integer origenesDistintos,
        List<Origen> origenes
) {

    /** Un archivo de carga que aporta filas a la celda, con su fecha de creación. */
    public record Origen(
            String nombre,
            LocalDateTime fechaCreacion
    ) {
    }

    /** Construye la respuesta a partir de las proyecciones por origen (lógica común H/QH). */
    public static MeasureCellOriginResponse from(
            Integer clienteId,
            Integer hora,
            Integer minuto,
            List<? extends MeasureCellOriginCount> counts
    ) {
        long totalRecords = counts.stream()
                .mapToLong(item -> item.getRegistros() == null ? 0L : item.getRegistros())
                .sum();
        List<Origen> origenes = counts.stream()
                .map(item -> new Origen(item.getOrigen(), item.getFechaCreacion()))
                .toList();
        return new MeasureCellOriginResponse(clienteId, hora, minuto, totalRecords, origenes.size(), origenes);
    }
}
