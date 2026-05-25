package com.com4energy.recordsapi.controller.medidas.h.dto;

/**
 * Proyeccion de cantidad de registros por origen para una celda (cliente/hora/dia).
 */
public interface MedidaHCellOriginCount {
    String getOrigen();
    Long getRegistros();
}

