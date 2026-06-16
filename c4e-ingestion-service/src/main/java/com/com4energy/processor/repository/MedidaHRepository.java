package com.com4energy.processor.repository;

import com.com4energy.persistence.medidas.medidah.BaseMedidaHRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MedidaHRepository extends BaseMedidaHRepository {

    /**
     * Pre-carga del upsert: medidas existentes cuya business key (clienteId, fecha) cae en los
     * conjuntos dados. La decisión exacta por par se hace en memoria. Usa uk_medida_h_business
     * (líder id_cliente) + partition pruning por fecha.
     */
    @Query("""
            select m.clienteId as clienteId, m.fecha as fecha, m.payloadHash as payloadHash, m.id as id
            from MedidaH m
            where m.clienteId in :clienteIds and m.fecha in :fechas
            """)
    List<ExistingMeasureView> findExistingByClienteIdsAndFechas(
            @Param("clienteIds") Collection<Integer> clienteIds,
            @Param("fechas") Collection<LocalDateTime> fechas
    );

}
