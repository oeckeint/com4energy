package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.common.Constants;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import com.com4energy.recordsapi.repository.MedidaQHRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedidaQHService {

    private final MedidaQHRepository medidaQHRepository;

    public List<MedidaQH> findAll() {
        return medidaQHRepository.findAll();
    }

    public List<MedidaQH> findLastNNoPage(Long clienteId, int n) {
        Pageable descPageable = PageRequest.of(0, n, Sort.by(Constants.FECHA).descending());
        return medidaQHRepository.findLastNNoPage(clienteId, descPageable);
    }

    public Page<MedidaQH> findAll(Long clienteId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (start == null && end == null) {
            // Si no hay fechas, traemos los últimos registros paginados
            return medidaQHRepository.findByFilters(clienteId, null, null, pageable);
        }
        return medidaQHRepository.findByFilters(clienteId, start, end, pageable);
    }

    public Optional<MedidaQH> findById(Long id) {
        return medidaQHRepository.findById(id);
    }

    public List<MedidaQH> findAllForCliente(Long idCliente) {
        return medidaQHRepository.findAllForCliente(idCliente);
    }
}
