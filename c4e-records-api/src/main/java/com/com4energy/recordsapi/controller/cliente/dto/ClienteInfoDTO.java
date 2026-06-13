package com.com4energy.recordsapi.controller.cliente.dto;

/**
 * Contrato de API para la info de cliente (desacoplado de la entidad {@code Cliente}
 * del kernel compartido). Target de la proyección en ClienteRepository.
 */
public record ClienteInfoDTO(
        Integer idCliente,
        String cups,
        String nombreCliente,
        String tarifa,
        Integer isDeleted) {
}
