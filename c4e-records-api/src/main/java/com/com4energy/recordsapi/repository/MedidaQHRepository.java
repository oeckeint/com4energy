package com.com4energy.recordsapi.repository;

import com.com4energy.persistence.medidas.medidaqh.BaseMedidaQHRepository;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MedidaQHRepository extends BaseMedidaQHRepository {

    String QUERY_FIND_BY_FILTERS = "select m from MedidaQH m"
            + " where (:clienteId is null or m.clienteId = :clienteId)"
            + " and ((cast(:start as timestamp) is null and cast(:end as timestamp) is null) or (m.fecha >= :start and m.fecha <= :end))";

    String QUERY_FIND_LAST_N = "select m from MedidaQH m where (:clienteId is null or m.clienteId = :clienteId) and m.fecha is not null order by m.fecha desc";

    String QUERY_FIND_ALL_FOR_CLIENTE = "select m from MedidaQH m where m.clienteId = :idCliente";

    @Query(QUERY_FIND_BY_FILTERS)
    Page<MedidaQH> findByFilters(@Param("clienteId") Long clienteId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 Pageable pageable);

    @Query(QUERY_FIND_LAST_N)
    List<MedidaQH> findLastN(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query(value = QUERY_FIND_LAST_N)
    List<MedidaQH> findLastNNoPage(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query(QUERY_FIND_ALL_FOR_CLIENTE)
    List<MedidaQH> findAllForCliente(@Param("idCliente") Long idCliente);
}
