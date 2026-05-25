package com.com4energy.recordsapi.controller.medidas.h.dto;

import java.util.List;

public record MedidaHCellOriginResponse(
        Integer clienteId,
        Integer hora,
        Long totalRegistros,
        Integer origenesDistintos,
        List<String> origenes
) {
}

