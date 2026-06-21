package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.common.Constants;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHCellOriginCount;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHCellOriginResponse;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHHourlyPoint;
import com.com4energy.persistence.medidas.medidah.MedidaH;
import com.com4energy.recordsapi.repository.MedidaHRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedidaHService {

    private final MedidaHRepository medidaHRepository;

    public List<MedidaH> findAll() {
        return medidaHRepository.findAll();
    }

    public List<MedidaH> findLastNNoPage(Long clienteId, int n) {
        Pageable descPageable = PageRequest.of(0, n, Sort.by(Constants.FECHA).descending());
        return medidaHRepository.findLastN(clienteId, descPageable);
    }

    public Page<MedidaH> findAll(Long clienteId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (start == null && end == null) {
            return medidaHRepository.findByFilters(clienteId, null, null, pageable);
        }
        return medidaHRepository.findByFilters(clienteId, start, end, pageable);
    }

    public Optional<MedidaH> findById(Long id) {
        return medidaHRepository.findById(id);
    }

    public List<MedidaH> findAllForCliente(Long idCliente) {
        return medidaHRepository.findAllForCliente(idCliente);
    }

    /**
     * Devuelve actent agregado (SUM) por cliente y hora para el día indicado.
     */
    public List<MedidaHHourlyPoint> findHourlyMatrix(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return medidaHRepository.findHourlyMatrix(start, end);
    }

    /**
     * Devuelve actent agregado (SUM) por cliente y hora, filtrando por tarifa (si se proporciona).
     */
    public List<MedidaHHourlyPoint> findHourlyMatrixByTarifa(LocalDate date, String tarifa) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return medidaHRepository.findHourlyMatrixByTarifa(start, end, tarifa);
    }

    public List<MedidaHHourlyPoint> findHourlyMatrixFiltered(LocalDate date, String tarifa, List<Integer> clientIds) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Integer> normalizedClientIds = clientIds == null
                ? List.of()
                : clientIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        boolean hasClients = !normalizedClientIds.isEmpty();
        boolean hasTarifa = tarifa != null && !tarifa.isBlank();

        if (hasTarifa && hasClients) {
            return medidaHRepository.findHourlyMatrixByTarifaAndClientIds(start, end, tarifa.trim(), normalizedClientIds);
        }
        if (hasTarifa) {
            return medidaHRepository.findHourlyMatrixByTarifa(start, end, tarifa.trim());
        }
        if (hasClients) {
            return medidaHRepository.findHourlyMatrixByClientIds(start, end, normalizedClientIds);
        }
        return medidaHRepository.findHourlyMatrix(start, end);
    }

    /**
     * Detalle de orígenes (archivos de carga) que aportan registros a una celda (cliente/hora/día).
     */
    public MedidaHCellOriginResponse findCellOrigins(LocalDate date, Integer clientId, Integer hour) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<MedidaHCellOriginCount> originCounts = medidaHRepository.findCellOrigins(clientId, hour, start, end);
        long totalRecords = originCounts.stream()
                .mapToLong(item -> item.getRegistros() == null ? 0L : item.getRegistros())
                .sum();
        List<MedidaHCellOriginResponse.Origen> origins = originCounts.stream()
                .map(item -> new MedidaHCellOriginResponse.Origen(item.getOrigen(), item.getFechaCreacion()))
                .toList();

        return new MedidaHCellOriginResponse(
                clientId,
                hour,
                totalRecords,
                origins.size(),
                origins
        );
    }
}
