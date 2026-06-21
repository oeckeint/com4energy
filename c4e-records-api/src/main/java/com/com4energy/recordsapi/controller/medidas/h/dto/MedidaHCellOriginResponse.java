package com.com4energy.recordsapi.controller.medidas.h.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MedidaHCellOriginResponse(
        Integer clienteId,
        Integer hora,
        Long totalRegistros,
        Integer origenesDistintos,
        List<Origen> origenes
) {

    /** Un archivo de carga que aporta filas a la celda, con su fecha de creacion. */
    public record Origen(
            String nombre,
            LocalDateTime fechaCreacion
    ) {
    }
}
