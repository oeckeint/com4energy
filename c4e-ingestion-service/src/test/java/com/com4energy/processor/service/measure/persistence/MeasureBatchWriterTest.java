package com.com4energy.processor.service.measure.persistence;

import com.com4energy.persistence.medidas.medidah.MedidaH;
import com.com4energy.processor.repository.MedidaHRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeasureBatchWriterTest {

    private final MeasureBatchWriter writer = new MeasureBatchWriter();

    @Test
    void insertBatchDelegatesToSaveAll() {
        MedidaHRepository repository = mock(MedidaHRepository.class);
        MedidaH entity = MedidaH.builder().clienteId(1).build();

        writer.insertBatch(repository, List.of(entity));

        verify(repository).saveAll(anyList());
    }

    /**
     * Regresión: el binary-split reintenta las MISMAS instancias y, tras un flush fallido, el id
     * ya quedó asignado en memoria (TSID/IDENTITY) sin que el rollback lo limpie. Si llega con id
     * != null, saveAll lo trataría como detached -> merge -> UPDATE de una fila inexistente ->
     * StaleObjectStateException. insertBatch debe resetear el id para forzar la ruta INSERT.
     */
    @Test
    void insertBatchResetsPreassignedIdSoRetryGoesThroughInsert() {
        MedidaHRepository repository = mock(MedidaHRepository.class);
        MedidaH entity = MedidaH.builder().clienteId(1).build();
        entity.setId(854625441969833620L); // id "sobreviviente" de un intento previo fallido

        // Capturamos el id en el momento exacto en que se invoca saveAll.
        Long[] idAtSaveTime = new Long[1];
        doAnswer(invocation -> {
            idAtSaveTime[0] = entity.getId();
            return List.of(entity);
        }).when(repository).saveAll(anyList());

        writer.insertBatch(repository, List.of(entity));

        assertNull(idAtSaveTime[0], "el id debe estar en null al llegar a saveAll (ruta INSERT)");
    }

    @Test
    void updateBatchCopiesNewValuesOntoManagedRowKeepingId() {
        MedidaHRepository repository = mock(MedidaHRepository.class);

        // Fila managed existente (la que se actualizará in-place).
        MedidaH managed = new MedidaH();
        managed.setId(555L);
        managed.setActent(1L);
        when(repository.findAllById(any())).thenReturn(List.of(managed));

        // Entidad detached con id existente + valores nuevos.
        MedidaH detached = MedidaH.builder()
                .clienteId(7)
                .fecha(LocalDateTime.of(2026, 4, 16, 1, 0))
                .tipoMedida(11)
                .banderaInvVer(1)
                .actent(99L)
                .qactent(0L).actsal(0L).qactsal(0L)
                .rq1(0L).qrq1(0L).rq2(0L).qrq2(0L).rq3(0L).qrq3(0L).rq4(0L).qrq4(0L)
                .medres1(0L).qmedres1(0L).medres2(0L).qmedres2(0L)
                .metodObt(1)
                .fileRecordId(42L)
                .payloadHash(new byte[]{1, 2, 3, 4, 5, 6, 7, 8})
                .payloadHashVersion(1)
                .build();
        detached.setId(555L);

        writer.updateBatch(repository, List.of(detached));

        // La fila managed conserva su id y recibe las columnas nuevas.
        assertEquals(555L, managed.getId().longValue());
        assertEquals(7, managed.getClienteId().intValue());
        assertEquals(99L, managed.getActent().longValue());
        assertEquals(42L, managed.getFileRecordId().longValue());
    }
}
