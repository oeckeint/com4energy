package com.com4energy.recordsapi.controller.medidas.qh.dto;

import java.math.BigDecimal;

public interface MedidaQHQuarterPoint {
    Integer getClienteId();

    Integer getHora();

    Integer getMinuto();

    BigDecimal getTotalActent();
}

