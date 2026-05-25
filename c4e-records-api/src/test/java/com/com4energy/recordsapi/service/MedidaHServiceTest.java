package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHHourlyPoint;
import com.com4energy.recordsapi.repository.MedidaHRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedidaHServiceTest {

    @Mock
    private MedidaHRepository medidaHRepository;

    @InjectMocks
    private MedidaHService medidaHService;

    @Test
    void findHourlyMatrixFiltered_withoutFilters_usesBaseQueryWithSameDateWindow() {
        LocalDate date = LocalDate.of(2026, 5, 17);
        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay();

        when(medidaHRepository.findHourlyMatrix(expectedStart, expectedEnd)).thenReturn(List.<MedidaHHourlyPoint>of());

        medidaHService.findHourlyMatrixFiltered(date, null, List.of());

        verify(medidaHRepository).findHourlyMatrix(eq(expectedStart), eq(expectedEnd));
    }

    @Test
    void findHourlyMatrixFiltered_onlyTarifa_usesTarifaQueryWithSameDateWindow() {
        LocalDate date = LocalDate.of(2026, 5, 17);
        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay();

        when(medidaHRepository.findHourlyMatrixByTarifa(expectedStart, expectedEnd, "T20")).thenReturn(List.<MedidaHHourlyPoint>of());

        medidaHService.findHourlyMatrixFiltered(date, " T20 ", List.of());

        verify(medidaHRepository).findHourlyMatrixByTarifa(eq(expectedStart), eq(expectedEnd), eq("T20"));
    }

    @Test
    void findHourlyMatrixFiltered_onlyClients_usesClientQueryWithSameDateWindow() {
        LocalDate date = LocalDate.of(2026, 5, 17);
        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay();
        List<Integer> expectedClientIds = List.of(1001, 1002);

        when(medidaHRepository.findHourlyMatrixByClientIds(expectedStart, expectedEnd, expectedClientIds))
                .thenReturn(List.<MedidaHHourlyPoint>of());

        medidaHService.findHourlyMatrixFiltered(date, "", Arrays.asList(1001, null, -1, 1002, 1001));

        verify(medidaHRepository).findHourlyMatrixByClientIds(eq(expectedStart), eq(expectedEnd), eq(expectedClientIds));
    }

    @Test
    void findHourlyMatrixFiltered_tarifaAndClients_usesDedicatedQueryWithSameDateWindow() {
        LocalDate date = LocalDate.of(2026, 5, 17);
        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay();
        List<Integer> expectedClientIds = List.of(1001, 1002);

        when(medidaHRepository.findHourlyMatrixByTarifaAndClientIds(expectedStart, expectedEnd, "T20", expectedClientIds))
                .thenReturn(List.<MedidaHHourlyPoint>of());

        medidaHService.findHourlyMatrixFiltered(date, "T20", List.of(1001, 1002));

        verify(medidaHRepository).findHourlyMatrixByTarifaAndClientIds(
                eq(expectedStart),
                eq(expectedEnd),
                eq("T20"),
                eq(expectedClientIds)
        );
    }
}


