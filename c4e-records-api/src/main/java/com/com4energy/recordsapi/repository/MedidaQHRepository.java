package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.dto.MedidaQH;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.time.LocalDateTime;

@Repository
public interface MedidaQHRepository extends JpaRepository<MedidaQH, Integer> {

        @Query("select m from MedidaQH m"
            + " where (:clienteId is null or m.id_cliente = :clienteId)"
            + " and ((cast(:start as timestamp) is null and cast(:end as timestamp) is null) or (m.fecha >= :start and m.fecha <= :end))")
        Page<MedidaQH> findByFilters(@Param("clienteId") Integer clienteId,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end,
                     Pageable pageable);

        @Query("select m from MedidaQH m where (:clienteId is null or m.id_cliente = :clienteId) and m.fecha is not null order by m.fecha desc")
        List<MedidaQH> findLastN(@Param("clienteId") Integer clienteId, org.springframework.data.domain.Pageable pageable);

        @Query(value = "select m from MedidaQH m where (:clienteId is null or m.id_cliente = :clienteId) and m.fecha is not null order by m.fecha desc")
        List<MedidaQH> findLastNNoPage(@Param("clienteId") Integer clienteId, org.springframework.data.domain.Pageable pageable);

        @Query("select m from MedidaQH m where m.id_cliente = :idCliente")
        List<MedidaQH> findAllForCliente(@Param("idCliente") Integer idCliente);
}
