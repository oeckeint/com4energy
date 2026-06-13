package com.com4energy.recordsapi.controller.medidas.qh.dto;

import java.util.List;

public record MedidaQHCellOriginResponse(
        Integer clienteId,
        Integer hora,
        Integer minuto,
        Long totalRegistros,
        Integer origenesDistintos,
        List<String> origenes
) {
}

