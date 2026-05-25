package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.controller.cliente.dto.ClienteInfoDTO;
import com.com4energy.recordsapi.domain.entity.cliente.Cliente;
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

    public Optional<Cliente> findById(Integer id) {
        return clienteRepository.findById(id);
    }

    public Cliente save(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public void deleteById(Integer id) {
        clienteRepository.deleteById(id);
    }

}
