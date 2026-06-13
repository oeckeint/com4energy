package com.com4energy.recordsapi.service;

import com.com4energy.persistence.medidas.medidacch.MedidaCCH;
import com.com4energy.recordsapi.repository.MedidaCCHRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedidaCCHService {

    private final MedidaCCHRepository medidaCCHRepository;

    public Page<MedidaCCH> findAll(Long clienteId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return medidaCCHRepository.findByFilters(clienteId, start, end, pageable);
    }

    public Optional<MedidaCCH> findById(Long id) {
        return medidaCCHRepository.findById(id);
    }
}
