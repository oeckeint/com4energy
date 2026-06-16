package com.com4energy.processor.service.measure.validation;

import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.service.measure.MeasureRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientExistsRecordValidatorTest {

    private static final String CUPS = "ES123456789012345678"; // 20 chars -> prefijo = el mismo

    @Test
    void returnsErrorWhenClientDoesNotExist() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of());

        var result = validator.validate(hourly(CUPS));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("No se encontró cliente"));
    }

    @Test
    void returnsErrorWhenMoreThanOneClientFound() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        when(clienteRepository.findLookupByCupsPrefixes(any()))
                .thenReturn(List.of(prefixView(CUPS, 1, null), prefixView(CUPS, 2, null)));

        var result = validator.validate(hourly(CUPS));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("más de un cliente"));
    }

    @Test
    void returnsErrorWhenClientHasNullId() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        when(clienteRepository.findLookupByCupsPrefixes(any()))
                .thenReturn(List.of(prefixView(CUPS, null, null)));

        var result = validator.validate(hourly(CUPS));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("no tiene id_cliente"));
    }

    @Test
    void returnsEmptyWhenClientExistsWithId() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        when(clienteRepository.findLookupByCupsPrefixes(any()))
                .thenReturn(List.of(prefixView(CUPS, 1, null)));

        var result = validator.validate(hourly(CUPS));

        assertTrue(result.isEmpty());
        assertEquals("CLIENT_EXISTS", validator.brokenRule());
    }

    @Test
    void resolvesClientsInSingleBatchQueryForRepeatedCups() {
        ClienteRepository clienteRepository = mock(ClienteRepository.class);
        ClientExistsRecordValidator validator = new ClientExistsRecordValidator(clienteRepository);

        when(clienteRepository.findLookupByCupsPrefixes(any()))
                .thenReturn(List.of(prefixView(CUPS, 1, null)));

        MeasureRecord.Hourly first = hourly(CUPS);
        MeasureRecord.Hourly second = hourly(CUPS);

        validator.beforeBatch(List.of(first, second));
        try {
            assertTrue(validator.validate(first).isEmpty());
            assertTrue(validator.validate(second).isEmpty());
        } finally {
            validator.afterBatch();
        }

        verify(clienteRepository, times(1)).findLookupByCupsPrefixes(any());
    }

    private MeasureRecord.Hourly hourly(String cups) {
        return new MeasureRecord.Hourly(
                cups,
                LocalDateTime.of(2025, 1, 1, 0, 0),
                11,
                0d,
                1d,
                0d,
                0d,
                0d,
                0d,
                0d,
                0d,
                0d,
                0d,
                0d,
                0d,
                0d,
                128d,
                0d,
                128d,
                0d,
                1,
                0,
                "P1D_0021_0894_20240104.0",
                cups + ";11;2025/01/01 00:00:00;..."
        );
    }

    private ClienteRepository.ClientePrefixView prefixView(String cupsPrefix, Integer id, String tarifa) {
        return new ClienteRepository.ClientePrefixView() {
            @Override
            public Integer getId() {
                return id;
            }

            @Override
            public String getTarifa() {
                return tarifa;
            }

            @Override
            public String getCupsPrefix() {
                return cupsPrefix;
            }
        };
    }
}
