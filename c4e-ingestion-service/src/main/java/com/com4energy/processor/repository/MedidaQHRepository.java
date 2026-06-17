package com.com4energy.processor.repository;

import com.com4energy.persistence.medidas.medidaqh.BaseMedidaQHRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MedidaQHRepository extends BaseMedidaQHRepository {

    /**
     * Pre-carga del upsert: medidas existentes cuya business key (clienteId, fecha) cae en los
     * conjuntos dados. La decisión exacta por par se hace en memoria. Usa uk_medida_qh_business
     * (líder id_cliente) + partition pruning por fecha. El join con file_records (por PK) trae la
     * procedencia (familia, revisión, iteración) para resolver la precedencia POR FILA.
     */
    @Query("""
            select m.clienteId as clienteId, m.fecha as fecha, m.payloadHash as payloadHash, m.id as id,
                   fr.measureVersion.sourceFamilyKey as sourceFamilyKey,
                   fr.measureVersion.revision as revision,
                   fr.measureVersion.processingIteration as processingIteration
            from MedidaQH m, FileRecord fr
            where fr.id = m.fileRecordId and m.clienteId in :clienteIds and m.fecha in :fechas
            """)
    List<ExistingMeasureView> findExistingByClienteIdsAndFechas(
            @Param("clienteIds") Collection<Integer> clienteIds,
            @Param("fechas") Collection<LocalDateTime> fechas
    );

}
