package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.controller.medidas.qh.dto.MedidaQHQuarterPoint;
import com.com4energy.recordsapi.repository.MedidaQHRepository;
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
class MedidaQHServiceTest {

    @Mock
    private MedidaQHRepository medidaQHRepository;

    @InjectMocks
    private MedidaQHService medidaQHService;

    @Test
    void findQuarterMatrixFiltered_withoutFilters_usesBaseQueryWithSameDateWindow() {
        LocalDate date = LocalDate.of(2026, 5, 17);
        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay();

        when(medidaQHRepository.findQuarterMatrix(expectedStart, expectedEnd)).thenReturn(List.<MedidaQHQuarterPoint>of());

        medidaQHService.findQuarterMatrixFiltered(date, null, List.of());

        verify(medidaQHRepository).findQuarterMatrix(eq(expectedStart), eq(expectedEnd));
    }

    @Test
    void findQuarterMatrixFiltered_onlyTarifa_usesTarifaQueryWithSameDateWindow() {
        LocalDate date = LocalDate.of(2026, 5, 17);
        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay();

        when(medidaQHRepository.findQuarterMatrixByTarifa(expectedStart, expectedEnd, "T20")).thenReturn(List.<MedidaQHQuarterPoint>of());

        medidaQHService.findQuarterMatrixFiltered(date, " T20 ", List.of());

        verify(medidaQHRepository).findQuarterMatrixByTarifa(eq(expectedStart), eq(expectedEnd), eq("T20"));
    }

    @Test
    void findQuarterMatrixFiltered_onlyClients_usesClientQueryWithSameDateWindow() {
        LocalDate date = LocalDate.of(2026, 5, 17);
        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay();
        List<Integer> expectedClientIds = List.of(1001, 1002);

        when(medidaQHRepository.findQuarterMatrixByClientIds(expectedStart, expectedEnd, expectedClientIds))
                .thenReturn(List.<MedidaQHQuarterPoint>of());

        medidaQHService.findQuarterMatrixFiltered(date, "", Arrays.asList(1001, null, -1, 1002, 1001));

        verify(medidaQHRepository).findQuarterMatrixByClientIds(eq(expectedStart), eq(expectedEnd), eq(expectedClientIds));
    }

    @Test
    void findQuarterMatrixFiltered_tarifaAndClients_usesDedicatedQueryWithSameDateWindow() {
        LocalDate date = LocalDate.of(2026, 5, 17);
        LocalDateTime expectedStart = date.atStartOfDay();
        LocalDateTime expectedEnd = date.plusDays(1).atStartOfDay();
        List<Integer> expectedClientIds = List.of(1001, 1002);

        when(medidaQHRepository.findQuarterMatrixByTarifaAndClientIds(expectedStart, expectedEnd, "T20", expectedClientIds))
                .thenReturn(List.<MedidaQHQuarterPoint>of());

        medidaQHService.findQuarterMatrixFiltered(date, "T20", List.of(1001, 1002));

        verify(medidaQHRepository).findQuarterMatrixByTarifaAndClientIds(
                eq(expectedStart),
                eq(expectedEnd),
                eq("T20"),
                eq(expectedClientIds)
        );
    }
}

