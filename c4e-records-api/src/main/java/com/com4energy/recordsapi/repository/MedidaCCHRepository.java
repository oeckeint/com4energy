package com.com4energy.recordsapi.repository;

import com.com4energy.persistence.medidas.medidacch.BaseMedidaCCHRepository;
import com.com4energy.persistence.medidas.medidacch.MedidaCCH;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MedidaCCHRepository extends BaseMedidaCCHRepository {

    String QUERY_FIND_BY_FILTERS = "select m from MedidaCCH m"
            + " where (:clienteId is null or m.clienteId = :clienteId)"
            + " and ((cast(:start as timestamp) is null and cast(:end as timestamp) is null)"
            + " or (m.fecha >= :start and m.fecha <= :end))";

    @Query(QUERY_FIND_BY_FILTERS)
    Page<MedidaCCH> findByFilters(@Param("clienteId") Long clienteId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  Pageable pageable);
}
