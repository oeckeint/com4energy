package com.com4energy.recordsapi.controller.medidas.qh.dto;

import java.math.BigDecimal;

/**
 * Proyección de actent agregado por cliente y cuarto de hora (slot de 15 min).
 * A diferencia de Medida H (24 buckets por hora), QH son 96 buckets por día:
 * la granularidad la fijan {@code hora} (0-23) + {@code minuto} (0/15/30/45).
 * Interfaz para compatibilidad con native query de Spring Data JPA.
 */
public interface MedidaQHQuarterHourPoint {
    Integer getClienteId();
    Integer getHora();
    Integer getMinuto();
    BigDecimal getTotalActent();
}
