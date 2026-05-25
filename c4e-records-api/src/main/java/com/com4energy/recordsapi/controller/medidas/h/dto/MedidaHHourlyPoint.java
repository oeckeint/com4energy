package com.com4energy.recordsapi.controller.medidas.h.dto;

import java.math.BigDecimal;

/**
 * Proyección de actent agregado por cliente y hora.
 * Interfaz para compatibilidad con native query de Spring Data JPA.
 */
public interface MedidaHHourlyPoint {
    Integer getClienteId();
    Integer getHora();
    BigDecimal getTotalActent();
}
