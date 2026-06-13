package com.com4energy.recordsapi.controller.cliente.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteInfoDTO {
    private Long idCliente;
    private String cups;
    private String nombreCliente;
    private String tarifa;
    private Short isDeleted;
}
