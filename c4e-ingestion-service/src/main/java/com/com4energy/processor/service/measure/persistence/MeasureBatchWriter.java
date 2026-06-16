package com.com4energy.processor.service.measure.persistence;

import com.com4energy.persistence.medidas.common.AbstractMeasure;
import com.com4energy.persistence.medidas.medidacch.MedidaCCH;
import com.com4energy.persistence.medidas.medidah.MedidaH;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Escribe un lote de medidas en su PROPIA transacción ({@code REQUIRES_NEW}).
 *
 * <p>Esto es lo que hace que el binary-split logre commit parcial real: cuando reintenta sub-lotes,
 * una violación de constraint revierte SOLO ese sub-lote y los buenos quedan commiteados. En una
 * única transacción, la primera violación marca la transacción como rollback-only y al commit final
 * se revierten también los buenos.
 *
 * <p>Debe ser un bean separado (no auto-invocación) para que el proxy de Spring aplique la
 * propagación {@code REQUIRES_NEW}.
 */
@Component
public class MeasureBatchWriter {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> void insertBatch(JpaRepository<T, ?> repository, List<T> batch) {
        // El binary-split reintenta las MISMAS instancias. Tras un flush fallido, el id ya fue
        // asignado en memoria (TSID/IDENTITY) y el rollback NO lo limpia. saveAll vería id != null,
        // trataría la entidad como detached -> merge -> UPDATE de una fila inexistente ->
        // StaleObjectStateException ("Row was updated or deleted by another transaction"), tumbando
        // también las filas buenas. Reseteamos el id para forzar la ruta INSERT (isNew == true) en
        // cada intento; para un insert el id es desechable y el generador lo reasigna.
        for (T entity : batch) {
            clearGeneratedId(entity);
        }
        repository.saveAll(batch);
        // El commit al retornar hace el flush (ids TSID ya asignados + JDBC batching).
    }

    private void clearGeneratedId(Object entity) {
        if (entity instanceof MedidaH medidaH) {
            medidaH.setId(null);
        } else if (entity instanceof MedidaQH medidaQH) {
            medidaQH.setId(null);
        } else if (entity instanceof MedidaCCH medidaCCH) {
            medidaCCH.setId(null);
        }
    }

    /**
     * UPDATE in-place por lote: carga las filas existentes (managed) por id en UNA consulta, copia
     * sobre ellas las columnas nuevas y deja que el commit (al retornar) haga el UPDATE batcheado.
     * La entidad detached del lote solo aporta valores + id.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T extends AbstractMeasure> void updateBatch(JpaRepository<T, Long> repository, List<T> batch) {
        List<Long> ids = new ArrayList<>(batch.size());
        for (T detached : batch) {
            ids.add(idOf(detached));
        }
        Map<Long, T> managedById = new HashMap<>();
        for (T managed : repository.findAllById(ids)) {
            managedById.put(idOf(managed), managed);
        }
        for (T detached : batch) {
            T managed = managedById.get(idOf(detached));
            if (managed != null) {
                copyMeasureFields(detached, managed);
            }
        }
    }

    /** Copia todas las columnas de medida (excepto el id) de una entidad a otra. */
    private void copyMeasureFields(AbstractMeasure from, AbstractMeasure to) {
        to.setClienteId(from.getClienteId());
        to.setTipoMedida(from.getTipoMedida());
        to.setFecha(from.getFecha());
        to.setBanderaInvVer(from.getBanderaInvVer());
        to.setActent(from.getActent());
        to.setQactent(from.getQactent());
        to.setActsal(from.getActsal());
        to.setQactsal(from.getQactsal());
        to.setRq1(from.getRq1());
        to.setQrq1(from.getQrq1());
        to.setRq2(from.getRq2());
        to.setQrq2(from.getQrq2());
        to.setRq3(from.getRq3());
        to.setQrq3(from.getQrq3());
        to.setRq4(from.getRq4());
        to.setQrq4(from.getQrq4());
        to.setMedres1(from.getMedres1());
        to.setQmedres1(from.getQmedres1());
        to.setMedres2(from.getMedres2());
        to.setQmedres2(from.getQmedres2());
        to.setMetodObt(from.getMetodObt());
        to.setFileRecordId(from.getFileRecordId());
        to.setPayloadHash(from.getPayloadHash());
        to.setPayloadHashVersion(from.getPayloadHashVersion());
    }

    private Long idOf(AbstractMeasure entity) {
        if (entity instanceof MedidaH medidaH) {
            return medidaH.getId();
        }
        if (entity instanceof MedidaQH medidaQH) {
            return medidaQH.getId();
        }
        return null;
    }
}
