package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.common.Constants;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import com.com4energy.recordsapi.controller.medidas.dto.MeasureCellOriginCount;
import com.com4energy.recordsapi.controller.medidas.dto.MeasureCellOriginResponse;
import com.com4energy.recordsapi.controller.medidas.qh.dto.MedidaQHQuarterHourPoint;
import com.com4energy.recordsapi.repository.MedidaQHRepository;
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

    /**
     * Matriz cuarto-horaria (96 slots) de actent agregado por cliente y slot, para el día indicado,
     * filtrando opcionalmente por tarifa y/o IDs de cliente.
     */
    public List<MedidaQHQuarterHourPoint> findQuarterHourMatrixFiltered(LocalDate date, String tarifa, List<Integer> clientIds) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Integer> normalizedClientIds = clientIds == null
                ? List.of()
                : clientIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        boolean hasClients = !normalizedClientIds.isEmpty();
        boolean hasTarifa = tarifa != null && !tarifa.isBlank();

        if (hasTarifa && hasClients) {
            return medidaQHRepository.findQuarterHourMatrixByTarifaAndClientIds(start, end, tarifa.trim(), normalizedClientIds);
        }
        if (hasTarifa) {
            return medidaQHRepository.findQuarterHourMatrixByTarifa(start, end, tarifa.trim());
        }
        if (hasClients) {
            return medidaQHRepository.findQuarterHourMatrixByClientIds(start, end, normalizedClientIds);
        }
        return medidaQHRepository.findQuarterHourMatrix(start, end);
    }

    /**
     * Detalle de orígenes (archivos de carga) que aportan registros a una celda QH
     * (cliente / hora / minuto / día).
     */
    public MeasureCellOriginResponse findCellOrigins(LocalDate date, Integer clientId, Integer hour, Integer minute) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<MeasureCellOriginCount> originCounts = medidaQHRepository.findCellOrigins(clientId, hour, minute, start, end);
        return MeasureCellOriginResponse.from(clientId, hour, minute, originCounts);
    }
}
