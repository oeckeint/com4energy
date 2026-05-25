package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.common.Constants;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHCellOriginCount;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHCellOriginResponse;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHHourlyPoint;
import com.com4energy.recordsapi.domain.entity.medidas.MedidaH;
import com.com4energy.recordsapi.repository.MedidaHRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public List<MedidaH> findLastNNoPage(Integer clienteId, int n) {
        Pageable descPageable = org.springframework.data.domain.PageRequest.of(0, n,
                org.springframework.data.domain.Sort.by(Constants.FECHA).descending());
        return medidaHRepository.findLastN(clienteId, descPageable);
    }

    public Page<MedidaH> findAll(Integer clienteId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (start == null && end == null) {
            return medidaHRepository.findByFilters(clienteId, null, null, pageable);
        }
        return medidaHRepository.findByFilters(clienteId, start, end, pageable);
    }

    public Optional<MedidaH> findById(Integer id) {
        return medidaHRepository.findById(id);
    }

    public MedidaH save(MedidaH medidaH) {
        return medidaHRepository.save(medidaH);
    }

    public void deleteById(Integer id) {
        medidaHRepository.deleteById(id);
    }

    public List<MedidaH> findAllForCliente(Integer idCliente) {
        return medidaHRepository.findAllForCliente(idCliente);
    }

    /**
     * Devuelve actent agregado (SUM) por cliente y hora para el día indicado.
     * Equivale a: SELECT id_cliente, HOUR(fecha), SUM(actent) FROM medida_h
     *             WHERE fecha >= :start AND fecha < :end
     *             GROUP BY id_cliente, HOUR(fecha)
     */
    public List<MedidaHHourlyPoint> findHourlyMatrix(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return medidaHRepository.findHourlyMatrix(start, end);
    }

    /**
     * Devuelve actent agregado (SUM) por cliente y hora, filtrando por tarifa (si se proporciona).
     * Hace JOIN con la tabla cliente para filtrar por tarifa del cliente.
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

    public MedidaHCellOriginResponse findCellOrigins(LocalDate date, Integer clientId, Integer hour) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<MedidaHCellOriginCount> originCounts = medidaHRepository.findCellOrigins(clientId, hour, start, end);
        long totalRecords = originCounts.stream().mapToLong(item -> item.getRegistros() == null ? 0L : item.getRegistros()).sum();
        List<String> origins = originCounts.stream().map(MedidaHCellOriginCount::getOrigen).toList();

        return new MedidaHCellOriginResponse(
                clientId,
                hour,
                totalRecords,
                origins.size(),
                origins
        );
    }

}
