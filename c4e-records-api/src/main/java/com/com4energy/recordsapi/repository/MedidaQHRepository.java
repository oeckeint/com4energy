package com.com4energy.recordsapi.repository;

import com.com4energy.persistence.medidas.medidaqh.BaseMedidaQHRepository;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import com.com4energy.recordsapi.controller.medidas.dto.MeasureCellOriginCount;
import com.com4energy.recordsapi.controller.medidas.qh.dto.MedidaQHQuarterHourPoint;
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

    // ---- Matriz cuarto-horaria (96 buckets/día): agrega por cliente + hora + minuto ----

    @Query(value =
            "SELECT m.id_cliente AS clienteId, HOUR(m.fecha) AS hora, MINUTE(m.fecha) AS minuto, SUM(m.actent) AS totalActent " +
            "FROM medida_qh m " +
            "WHERE m.fecha >= :start AND m.fecha < :end " +
            "GROUP BY m.id_cliente, HOUR(m.fecha), MINUTE(m.fecha) " +
            "ORDER BY m.id_cliente, HOUR(m.fecha), MINUTE(m.fecha)",
            nativeQuery = true)
    List<MedidaQHQuarterHourPoint> findQuarterHourMatrix(@Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    @Query(value =
            "SELECT m.id_cliente AS clienteId, HOUR(m.fecha) AS hora, MINUTE(m.fecha) AS minuto, SUM(m.actent) AS totalActent " +
            "FROM medida_qh m " +
            "INNER JOIN cliente c ON m.id_cliente = c.id_cliente " +
            "WHERE m.fecha >= :start AND m.fecha < :end " +
            "AND (:tarifa IS NULL OR c.tarifa = :tarifa) " +
            "GROUP BY m.id_cliente, HOUR(m.fecha), MINUTE(m.fecha) " +
            "ORDER BY m.id_cliente, HOUR(m.fecha), MINUTE(m.fecha)",
            nativeQuery = true)
    List<MedidaQHQuarterHourPoint> findQuarterHourMatrixByTarifa(@Param("start") LocalDateTime start,
                                                                 @Param("end") LocalDateTime end,
                                                                 @Param("tarifa") String tarifa);

    @Query(value =
            "SELECT m.id_cliente AS clienteId, HOUR(m.fecha) AS hora, MINUTE(m.fecha) AS minuto, SUM(m.actent) AS totalActent " +
            "FROM medida_qh m " +
            "WHERE m.fecha >= :start AND m.fecha < :end " +
            "AND m.id_cliente IN (:clientIds) " +
            "GROUP BY m.id_cliente, HOUR(m.fecha), MINUTE(m.fecha) " +
            "ORDER BY m.id_cliente, HOUR(m.fecha), MINUTE(m.fecha)",
            nativeQuery = true)
    List<MedidaQHQuarterHourPoint> findQuarterHourMatrixByClientIds(@Param("start") LocalDateTime start,
                                                                    @Param("end") LocalDateTime end,
                                                                    @Param("clientIds") List<Integer> clientIds);

    @Query(value =
            "SELECT m.id_cliente AS clienteId, HOUR(m.fecha) AS hora, MINUTE(m.fecha) AS minuto, SUM(m.actent) AS totalActent " +
            "FROM medida_qh m " +
            "INNER JOIN cliente c ON m.id_cliente = c.id_cliente " +
            "WHERE m.fecha >= :start AND m.fecha < :end " +
            "AND c.tarifa = :tarifa " +
            "AND m.id_cliente IN (:clientIds) " +
            "GROUP BY m.id_cliente, HOUR(m.fecha), MINUTE(m.fecha) " +
            "ORDER BY m.id_cliente, HOUR(m.fecha), MINUTE(m.fecha)",
            nativeQuery = true)
    List<MedidaQHQuarterHourPoint> findQuarterHourMatrixByTarifaAndClientIds(@Param("start") LocalDateTime start,
                                                                             @Param("end") LocalDateTime end,
                                                                             @Param("tarifa") String tarifa,
                                                                             @Param("clientIds") List<Integer> clientIds);

    // ---- Orígenes de una celda QH (cliente / hora / minuto / día) ----

    @Query(value =
            "SELECT COALESCE(NULLIF(TRIM(fr.original_filename), ''), NULLIF(TRIM(fr.final_filename), ''), 'Origen no informado') AS origen, "
            + "COUNT(*) AS registros, "
            + "fr.created_at AS fechaCreacion "
            + "FROM medida_qh m "
            + "JOIN file_records fr ON fr.id = m.id_file_record "
            + "WHERE m.id_cliente = :clientId "
            + "AND m.fecha >= :start AND m.fecha < :end "
            + "AND HOUR(m.fecha) = :hour AND MINUTE(m.fecha) = :minute "
            + "GROUP BY fr.id "
            + "ORDER BY registros DESC, origen ASC",
            nativeQuery = true)
    List<MeasureCellOriginCount> findCellOrigins(@Param("clientId") Integer clientId,
                                                 @Param("hour") Integer hour,
                                                 @Param("minute") Integer minute,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
