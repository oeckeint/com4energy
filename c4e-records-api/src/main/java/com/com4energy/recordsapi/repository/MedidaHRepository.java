package com.com4energy.recordsapi.repository;

import com.com4energy.persistence.medidas.medidah.BaseMedidaHRepository;
import com.com4energy.persistence.medidas.medidah.MedidaH;
import com.com4energy.recordsapi.controller.medidas.dto.MeasureCellOriginCount;
import com.com4energy.recordsapi.controller.medidas.h.dto.MedidaHHourlyPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MedidaHRepository extends BaseMedidaHRepository {

    String QUERY_FIND_BY_FILTERS = "select m from MedidaH m"
            + " where (:clienteId is null or m.clienteId = :clienteId)"
            + " and ((cast(:start as timestamp) is null and cast(:end as timestamp) is null)"
            + " or (m.fecha >= :start and m.fecha <= :end))";

    String QUERY_FIND_LAST_N = "select m from MedidaH m where (:clienteId is null or m.clienteId = :clienteId) and m.fecha is not null order by m.fecha desc";

    String QUERY_FIND_ALL_FOR_CLIENTE = "select m from MedidaH m where m.clienteId = :idCliente";

    @Query(QUERY_FIND_BY_FILTERS)
    Page<MedidaH> findByFilters(@Param("clienteId") Long clienteId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                Pageable pageable);

    @Query(QUERY_FIND_LAST_N)
    List<MedidaH> findLastN(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query(QUERY_FIND_ALL_FOR_CLIENTE)
    List<MedidaH> findAllForCliente(@Param("idCliente") Long idCliente);

    @Query(value =
            "SELECT m.id_cliente AS clienteId, HOUR(m.fecha) AS hora, SUM(m.actent) AS totalActent " +
            "FROM medida_h m " +
            "WHERE m.fecha >= :start AND m.fecha < :end " +
            "GROUP BY m.id_cliente, HOUR(m.fecha) " +
            "ORDER BY m.id_cliente, HOUR(m.fecha)",
            nativeQuery = true)
    List<MedidaHHourlyPoint> findHourlyMatrix(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query(value =
            "SELECT m.id_cliente AS clienteId, HOUR(m.fecha) AS hora, SUM(m.actent) AS totalActent " +
            "FROM medida_h m " +
            "INNER JOIN cliente c ON m.id_cliente = c.id_cliente " +
            "WHERE m.fecha >= :start AND m.fecha < :end " +
            "AND (:tarifa IS NULL OR c.tarifa = :tarifa) " +
            "GROUP BY m.id_cliente, HOUR(m.fecha) " +
            "ORDER BY m.id_cliente, HOUR(m.fecha)",
            nativeQuery = true)
    List<MedidaHHourlyPoint> findHourlyMatrixByTarifa(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end,
                                                      @Param("tarifa") String tarifa);

    @Query(value =
            "SELECT m.id_cliente AS clienteId, HOUR(m.fecha) AS hora, SUM(m.actent) AS totalActent " +
            "FROM medida_h m " +
            "WHERE m.fecha >= :start AND m.fecha < :end " +
            "AND m.id_cliente IN (:clientIds) " +
            "GROUP BY m.id_cliente, HOUR(m.fecha) " +
            "ORDER BY m.id_cliente, HOUR(m.fecha)",
            nativeQuery = true)
    List<MedidaHHourlyPoint> findHourlyMatrixByClientIds(@Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end,
                                                         @Param("clientIds") List<Integer> clientIds);

    @Query(value =
            "SELECT m.id_cliente AS clienteId, HOUR(m.fecha) AS hora, SUM(m.actent) AS totalActent " +
            "FROM medida_h m " +
            "INNER JOIN cliente c ON m.id_cliente = c.id_cliente " +
            "WHERE m.fecha >= :start AND m.fecha < :end " +
            "AND c.tarifa = :tarifa " +
            "AND m.id_cliente IN (:clientIds) " +
            "GROUP BY m.id_cliente, HOUR(m.fecha) " +
            "ORDER BY m.id_cliente, HOUR(m.fecha)",
            nativeQuery = true)
    List<MedidaHHourlyPoint> findHourlyMatrixByTarifaAndClientIds(@Param("start") LocalDateTime start,
                                                                  @Param("end") LocalDateTime end,
                                                                  @Param("tarifa") String tarifa,
                                                                  @Param("clientIds") List<Integer> clientIds);

    /**
     * Orígenes (archivos de carga) que aportan filas a una celda (cliente/hora/día).
     * El origen ya no es una columna de medida_h: se deriva del file_record apuntado por
     * id_file_record (nombre original del archivo; si falta, el final; si tampoco, etiqueta neutra).
     */
    @Query(value =
            "SELECT COALESCE(NULLIF(TRIM(fr.original_filename), ''), NULLIF(TRIM(fr.final_filename), ''), 'Origen no informado') AS origen, "
            + "COUNT(*) AS registros, "
            + "fr.created_at AS fechaCreacion "
            + "FROM medida_h m "
            + "JOIN file_records fr ON fr.id = m.id_file_record "
            + "WHERE m.id_cliente = :clientId "
            + "AND m.fecha >= :start AND m.fecha < :end "
            + "AND HOUR(m.fecha) = :hour "
            + "GROUP BY fr.id "
            + "ORDER BY registros DESC, origen ASC",
            nativeQuery = true)
    List<MeasureCellOriginCount> findCellOrigins(@Param("clientId") Integer clientId,
                                                 @Param("hour") Integer hour,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
