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
        managed.setActent(1);
        when(repository.findAllById(any())).thenReturn(List.of(managed));

        // Entidad detached con id existente + valores nuevos.
        MedidaH detached = MedidaH.builder()
                .clienteId(7)
                .fecha(LocalDateTime.of(2026, 4, 16, 1, 0))
                .tipoMedida((short) 11)
                .banderaInvVer(true)
                .actent(99)
                .qactent(0).actsal(0).qactsal(0)
                .rq1(0).qrq1(0).rq2(0).qrq2(0).rq3(0).qrq3(0).rq4(0).qrq4(0)
                .medres1(0).qmedres1(0).medres2(0).qmedres2(0)
                .metodObt((short) 1)
                .fileRecordId(42L)
                .payloadHash(new byte[]{1, 2, 3, 4, 5, 6, 7, 8})
                .payloadHashVersion(1)
                .build();
        detached.setId(555L);

        writer.updateBatch(repository, List.of(detached));

        // La fila managed conserva su id y recibe las columnas nuevas.
        assertEquals(555L, managed.getId().longValue());
        assertEquals(7, managed.getClienteId().intValue());
        assertEquals(99, managed.getActent().intValue());
        assertEquals(42L, managed.getFileRecordId().longValue());
    }
}
