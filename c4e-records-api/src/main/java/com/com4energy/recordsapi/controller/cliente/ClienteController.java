package com.com4energy.recordsapi.controller.cliente;

import com.com4energy.recordsapi.controller.cliente.dto.ClienteInfoDTO;
import com.com4energy.recordsapi.controller.common.ResponseHelper;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cliente")
public class ClienteController {

    private final ClienteService clienteService;

    /**
     * Obtiene información de cliente por ID con una consulta proyectada y liviana.
     *
     * @param idCliente ID del cliente
     * @return Información del cliente (idCliente, cups, nombreCliente, tarifa, isDeleted)
     */
    @GetMapping("/{idCliente}")
    public ResponseEntity<ClienteInfoDTO> getClienteInfo(@PathVariable("idCliente") Integer idCliente) {
        ClienteInfoDTO dto = clienteService.findClienteInfoById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Cliente con ID %d no encontrado", idCliente)
                ));

        return ResponseHelper.ok(dto);
    }
}
