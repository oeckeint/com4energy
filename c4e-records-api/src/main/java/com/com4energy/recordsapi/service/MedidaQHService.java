package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.dto.MedidaQH;

import com.com4energy.recordsapi.repository.MedidaQHRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MedidaQHService {

    private static final String FIELD_FECHA = "fecha";

    private final MedidaQHRepository medidaQHRepository;

    public List<MedidaQH> findAll() {
        return medidaQHRepository.findAll();
    }

    public List<MedidaQH> findLastNNoPage(Integer clienteId, int n) {
        Pageable descPageable = org.springframework.data.domain.PageRequest.of(0, n, org.springframework.data.domain.Sort.by(FIELD_FECHA).descending());
        return medidaQHRepository.findLastNNoPage(clienteId, descPageable);
    }

    public Page<MedidaQH> findAll(Integer clienteId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (start == null && end == null) {
            // Si no hay fechas, traemos los últimos registros paginados
            return medidaQHRepository.findByFilters(clienteId, null, null, pageable);
        }
        return medidaQHRepository.findByFilters(clienteId, start, end, pageable);
    }

    public Optional<MedidaQH> findById(int id) {
        return medidaQHRepository.findById(id);
    }

    public MedidaQH save(MedidaQH medidaQH) {
        return medidaQHRepository.save(medidaQH);
    }

    public void deleteById(int id) {
        medidaQHRepository.deleteById(id);
    }

    public List<MedidaQH> findAllForCliente(Integer idCliente) {
        return medidaQHRepository.findAllForCliente(idCliente);
    }
}
