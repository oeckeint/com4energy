package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.domain.entity.cliente.Tarifa;
import com.com4energy.recordsapi.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TarifaService {

    // En el esquema legacy de SGE, status = "1" representa tarifa activa.
    private static final String ACTIVE_STATUS = "1";

    private final TarifaRepository tarifaRepository;

    public List<String> findAvailableTarifas() {
        List<Tarifa> activeTarifas = tarifaRepository.findByStatusOrderByNombreTarifaAsc(ACTIVE_STATUS);
        List<Tarifa> source = activeTarifas.isEmpty()
                ? tarifaRepository.findAllByOrderByNombreTarifaAsc()
                : activeTarifas;

        return source.stream()
                .map(Tarifa::getNombreTarifa)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}

