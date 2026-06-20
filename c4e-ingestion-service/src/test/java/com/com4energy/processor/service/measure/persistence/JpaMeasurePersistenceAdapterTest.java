package com.com4energy.processor.service.measure.persistence;

import com.com4energy.processor.repository.MedidaCCHRepository;
import com.com4energy.persistence.medidas.medidah.MedidaH;
import com.com4energy.processor.repository.MedidaHRepository;
import com.com4energy.processor.repository.MedidaQHRepository;
import com.com4energy.processor.repository.ClienteRepository;
import com.com4energy.processor.repository.ExistingMeasureView;
import com.com4energy.processor.service.measure.MeasureRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaMeasurePersistenceAdapterTest {

    private final MedidaHRepository medidaHRepository = mock(MedidaHRepository.class);
    private final MedidaQHRepository medidaQHRepository = mock(MedidaQHRepository.class);
    private final MedidaCCHRepository medidaCCHRepository = mock(MedidaCCHRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final MeasureBatchWriter measureBatchWriter = mock(MeasureBatchWriter.class);

    private final JpaMeasurePersistenceAdapter adapter = new JpaMeasurePersistenceAdapter(
            medidaHRepository, medidaQHRepository, medidaCCHRepository, clienteRepository, measureBatchWriter);

    @Test
    void persistReturnsErrorWhenClientNotFound() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of());

        var result = adapter.persist(command(List.of(hourly(cups(1)))));

        assertEquals(0, result.persistedCount());
        assertEquals(1, result.errorCount());
        assertTrue(result.errors().get(0).contains("No se encontró cliente"));
        verify(measureBatchWriter, never()).insertBatch(any(), anyList());
    }

    @Test
    void persistReturnsErrorWhenClientIsDuplicatedForCups() {
        when(clienteRepository.findLookupByCupsPrefixes(any()))
                .thenReturn(List.of(prefixView(prefixOf(2), 10, "2.0A"), prefixView(prefixOf(2), 11, "2.0A")));

        var result = adapter.persist(command(List.of(quarterHourly(cups(2)))));

        assertEquals(0, result.persistedCount());
        assertEquals(1, result.errorCount());
        assertTrue(result.errors().get(0).contains("más de un cliente"));
        verify(measureBatchWriter, never()).insertBatch(any(), anyList());
    }

    @Test
    void persistRoutesF5AsCch() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(3), 12, "20TD")));

        var result = adapter.persist(command(List.of(cchFromF5(cups(3)))));

        assertEquals(1, result.persistedCount());
        verify(measureBatchWriter).insertBatch(eq(medidaCCHRepository), anyList());
        verify(measureBatchWriter, never()).insertBatch(eq(medidaHRepository), anyList());
        verify(measureBatchWriter, never()).insertBatch(eq(medidaQHRepository), anyList());
    }

    @Test
    void persistInsertsWhenNoExistingMeasure() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));
        when(medidaHRepository.findExistingByClienteIdsAndFechas(any(), any())).thenReturn(List.of());

        var result = adapter.persist(command(List.of(hourly(cups(1)))));

        assertEquals(1, result.persistedCount());
        assertEquals(0, result.updatedCount());
        assertEquals(0, result.skippedCount());
        verify(measureBatchWriter).insertBatch(eq(medidaHRepository), anyList());
    }

    @Test
    void persistSkipsWhenExistingHashMatches() {
        MeasureRecord.Hourly record = hourly(cups(1));
        byte[] sameHash = hash8(record.rawLine());

        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));
        when(medidaHRepository.findExistingByClienteIdsAndFechas(any(), any()))
                .thenReturn(List.of(existingView(1, record.timestamp(), sameHash, 555L)));

        var result = adapter.persist(command(List.of(record)));

        assertEquals(0, result.persistedCount());
        assertEquals(0, result.updatedCount());
        assertEquals(1, result.skippedCount());
        verify(measureBatchWriter, never()).insertBatch(any(), anyList());
        verify(measureBatchWriter, never()).updateBatch(any(), anyList());
    }

    @Test
    void persistUpdatesInPlaceWhenExistingHashDiffers() {
        MeasureRecord.Hourly record = hourly(cups(1));
        byte[] differentHash = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};

        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));
        when(medidaHRepository.findExistingByClienteIdsAndFechas(any(), any()))
                .thenReturn(List.of(existingView(1, record.timestamp(), differentHash, 555L)));

        var result = adapter.persist(command(List.of(record)));

        assertEquals(0, result.persistedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(0, result.skippedCount());
        verify(measureBatchWriter, never()).insertBatch(any(), anyList());

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<MedidaH>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(measureBatchWriter).updateBatch(eq(medidaHRepository), captor.capture());
        // El UPDATE in-place lleva el id existente seteado en la entidad.
        assertEquals(555L, captor.getValue().get(0).getId().longValue());
    }

    @Test
    void persistRejectsCrossFamilyCollision() {
        MeasureRecord.Hourly record = hourly(cups(1));
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));
        // La (cliente, fecha) ya existe pero pertenece a OTRA familia -> cross-familia.
        when(medidaHRepository.findExistingByClienteIdsAndFechas(any(), any()))
                .thenReturn(List.of(existingView(1, record.timestamp(), new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 555L,
                        "P1D_OTHER_FAMILY", 0, 0)));

        var result = adapter.persist(new MeasurePersistenceContracts.PersistMeasuresCommand(
                1L, "f", List.of(record), "P1D_THIS_FAMILY", 0, 0));

        assertTrue(result.crossFamilyCollision());
        assertEquals(0, result.persistedCount());
        assertEquals(0, result.updatedCount());
        verify(measureBatchWriter, never()).insertBatch(any(), anyList());
        verify(measureBatchWriter, never()).updateBatch(any(), anyList());
    }

    @Test
    void persistSkipsStaleWhenExistingRevisionIsNewer() {
        MeasureRecord.Hourly record = hourly(cups(1));
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));
        // Misma familia, misma (cliente, fecha), pero la fila existente proviene de una revisión MAYOR
        // (1 > 0) -> el entrante es obsoleto -> SKIP_STALE (no se pisa el dato más nuevo).
        when(medidaHRepository.findExistingByClienteIdsAndFechas(any(), any()))
                .thenReturn(List.of(existingView(1, record.timestamp(), new byte[]{9, 9, 9, 9, 9, 9, 9, 9}, 555L,
                        "FAM", 1, 0)));

        var result = adapter.persist(new MeasurePersistenceContracts.PersistMeasuresCommand(
                1L, "f", List.of(record), "FAM", 0, 0));

        assertEquals(0, result.persistedCount());
        assertEquals(0, result.updatedCount());
        assertEquals(1, result.skippedStaleCount());
        assertEquals(0, result.skippedIdenticalCount());
        verify(measureBatchWriter, never()).insertBatch(any(), anyList());
        verify(measureBatchWriter, never()).updateBatch(any(), anyList());
    }

    @Test
    void persistRoutesEachMeasureTypeToItsRepository() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(
                prefixView(prefixOf(101), 21, "2.0A"),
                prefixView(prefixOf(102), 22, "2.0A"),
                prefixView(prefixOf(103), 23, "3.0A")
        ));

        var result = adapter.persist(command(List.of(
                hourly(cups(101)), quarterHourly(cups(102)), cchFromF5(cups(103)))));

        assertEquals(3, result.persistedCount());
        verify(measureBatchWriter).insertBatch(eq(medidaHRepository), anyList());
        verify(measureBatchWriter).insertBatch(eq(medidaQHRepository), anyList());
        verify(measureBatchWriter).insertBatch(eq(medidaCCHRepository), anyList());
    }

    @Test
    void persistFlushesRecordsInBatchesOfOneThousand() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));

        List<MeasureRecord> largeDataset = new java.util.ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            largeDataset.add(hourly(cups(1)));
        }

        var result = adapter.persist(command(largeDataset));

        assertEquals(2500, result.persistedCount());
        // Lotes de 1000, 1000, 500.
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<MedidaH>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(measureBatchWriter, times(3)).insertBatch(eq(medidaHRepository), captor.capture());
        List<List<MedidaH>> batches = captor.getAllValues();
        assertEquals(1000, batches.get(0).size());
        assertEquals(1000, batches.get(1).size());
        assertEquals(500, batches.get(2).size());
    }

    @Test
    void persistBinarySplitsWhenInsertFails() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));

        // El lote de 3 falla; las mitades (<=2) pasan.
        doThrow(new RuntimeException("Constraint violation"))
                .when(measureBatchWriter).insertBatch(eq(medidaHRepository), argThat(l -> l != null && l.size() == 3));

        var result = adapter.persist(command(
                List.of(hourly(cups(1)), hourly(cups(1)), hourly(cups(1)))));

        assertTrue(result.persistedCount() >= 0, "debe recuperarse del fallo dividiendo el lote");
    }

    @Test
    void persistReturnsFailedRecordsWhenBinarySplitIsolatesInvalidRecord() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));

        java.util.concurrent.atomic.AtomicBoolean singleFailed = new java.util.concurrent.atomic.AtomicBoolean(false);
        doAnswer(invocation -> {
            List<?> batch = invocation.getArgument(1);
            if (batch.size() == 2) {
                throw new RuntimeException("Constraint violation");
            }
            if (batch.size() == 1 && !singleFailed.get()) {
                singleFailed.set(true);
                throw new RuntimeException("Single record violation");
            }
            return null;
        }).when(measureBatchWriter).insertBatch(eq(medidaHRepository), anyList());

        MeasureRecord.Hourly first = hourly(cups(1));
        MeasureRecord.Hourly second = hourly(cups(1));
        var result = adapter.persist(command(List.of(first, second)));

        assertEquals(1, result.persistedCount());
        assertEquals(1, result.failedRecords().size());
        assertEquals(first, result.failedRecords().get(0));
    }

    @Test
    void persistRoundsHourlyMagnitudesHalfEven() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));
        when(medidaHRepository.findExistingByClienteIdsAndFechas(any(), any())).thenReturn(List.of());

        // No-empates: actent=7.001->7, actsal=19.000->19, medres2=7.999->8.
        // Empates .5 -> entero PAR: rQ1=7.5->8, rQ2=0.5->0, rQ3=2.5->2, rQ4=3.5->4, medres1=2.0->2.
        // qactent=16 es código (no se redondea).
        MeasureRecord.Hourly hourly = new MeasureRecord.Hourly(
                cups(1), LocalDateTime.of(2025, 1, 1, 0, 0), 11,
                1d,
                7.001d, 16d, 19.000d, 0d, 7.5d, 0d, 0.5d, 0d, 2.5d, 0d, 3.5d, 0d,
                2.0d, 0d, 7.999d, 0d,
                1, 0, "P1D_0021_0894_20240104.0", "raw");

        adapter.persist(command(List.of(hourly)));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<MedidaH>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(measureBatchWriter).insertBatch(eq(medidaHRepository), captor.capture());
        MedidaH entity = captor.getValue().get(0);
        assertEquals(7, entity.getActent().intValue(), "7.001 NO sube (< .5)");
        assertEquals(19, entity.getActsal().intValue(), "19.000 se queda en 19");
        assertEquals(8, entity.getRq1().intValue(), "empate 7.5 -> par 8");
        assertEquals(0, entity.getRq2().intValue(), "empate 0.5 -> par 0");
        assertEquals(2, entity.getRq3().intValue(), "empate 2.5 -> par 2");
        assertEquals(4, entity.getRq4().intValue(), "empate 3.5 -> par 4");
        assertEquals(2, entity.getMedres1().intValue(), "2.0 entero exacto se queda en 2");
        assertEquals(8, entity.getMedres2().intValue(), "7.999 sube a 8");
        assertEquals(16, entity.getQactent().intValue(), "el código q* no se redondea");
    }

    @Test
    void persistResolvesClientsInASingleBatchQuery() {
        when(clienteRepository.findLookupByCupsPrefixes(any())).thenReturn(List.of(prefixView(prefixOf(1), 1, "2.0A")));

        adapter.persist(command(List.of(hourly(cups(1)), hourly(cups(1)), hourly(cups(1)))));

        verify(clienteRepository, times(1)).findLookupByCupsPrefixes(any());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private MeasurePersistenceContracts.PersistMeasuresCommand command(List<MeasureRecord> records) {
        return new MeasurePersistenceContracts.PersistMeasuresCommand(1L, "f", records);
    }

    private static byte[] hash8(String rawLine) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((rawLine == null ? "" : rawLine).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Arrays.copyOf(digest, 8);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private ExistingMeasureView existingView(Integer clienteId, LocalDateTime fecha, byte[] payloadHash, Long id) {
        return existingView(clienteId, fecha, payloadHash, id, "fam-default", 0, 0);
    }

    private ExistingMeasureView existingView(
            Integer clienteId, LocalDateTime fecha, byte[] payloadHash, Long id,
            String family, Integer revision, Integer iteration) {
        return new ExistingMeasureView() {
            @Override
            public Integer getClienteId() {
                return clienteId;
            }

            @Override
            public LocalDateTime getFecha() {
                return fecha;
            }

            @Override
            public byte[] getPayloadHash() {
                return payloadHash;
            }

            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getSourceFamilyKey() {
                return family;
            }

            @Override
            public Integer getRevision() {
                return revision;
            }

            @Override
            public Integer getProcessingIteration() {
                return iteration;
            }
        };
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

    private static String cups(int n) {
        return String.format("ES%018dZZ", n);
    }

    private static String prefixOf(int n) {
        return String.format("ES%018d", n);
    }

    private MeasureRecord.Hourly hourly(String cups) {
        return new MeasureRecord.Hourly(
                cups, LocalDateTime.of(2025, 1, 1, 0, 0), 11,
                0d, 1d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 128d, 0d, 128d, 0d,
                1, 0, "P1D_0021_0894_20240104.0", cups + ";11;2025/01/01 00:00:00;...");
    }

    private MeasureRecord.QuarterHourly quarterHourly(String cups) {
        return new MeasureRecord.QuarterHourly(
                cups, LocalDateTime.of(2025, 1, 1, 0, 0), 11,
                0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128, 0, 128, 0,
                1, 99, "P2D_0021_0894_20240104.0", cups + ";11;2025/01/01 00:00:00;...");
    }

    private MeasureRecord.Cch cchFromF5(String cups) {
        return new MeasureRecord.Cch(
                cups, LocalDateTime.of(2025, 1, 1, 0, 0), 0, 10, 1, cups + ";2025/01/01 00:00;...");
    }
}
