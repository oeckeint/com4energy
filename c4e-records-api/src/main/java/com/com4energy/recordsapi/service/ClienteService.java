package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.controller.cliente.dto.ClienteInfoDTO;
import com.com4energy.recordsapi.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public Optional<ClienteInfoDTO> findClienteInfoById(Integer id) {
        return clienteRepository.findClienteInfoById(id);
    }
}
