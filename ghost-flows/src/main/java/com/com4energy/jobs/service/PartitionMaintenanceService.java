package com.com4energy.jobs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

/**
 * Mantiene las particiones anuales por fecha de las tablas de medidas.
 *
 * <p>Las tablas medida_h / medida_qh están particionadas por RANGE COLUMNS(fecha)
 * con una partición por año y una partición catch-all {@code pfuture} (MAXVALUE).
 * Este servicio reorganiza {@code pfuture} para materializar la partición del año
 * entrante ANTES de que llegue su data, evitando que las filas caigan en el
 * catch-all (de donde ya no se podrían dropear por año).
 *
 * <p>Es <b>aditivo e idempotente</b>: nunca borra datos. El DROP de años vencidos
 * se deja como operación manual y deliberada (es destructivo).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartitionMaintenanceService {

    private static final List<String> PARTITIONED_TABLES = List.of("medida_h", "medida_qh");
    private static final String FUTURE_PARTITION = "pfuture";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Asegura que exista la partición del próximo año en cada tabla de medidas.
     */
    public void ensureUpcomingPartitions() {
        int targetYear = Year.now().getValue() + 1;
        for (String table : PARTITIONED_TABLES) {
            try {
                ensureYearPartition(table, targetYear);
            } catch (RuntimeException ex) {
                // No abortamos el resto de tablas si una falla.
                log.error("Fallo asegurando la partición {} en {}", "p" + targetYear, table, ex);
            }
        }
    }

    private void ensureYearPartition(String table, int year) {
        String partitionName = "p" + year;

        if (partitionExists(table, partitionName)) {
            log.debug("Partición {} ya existe en {}; nada que hacer", partitionName, table);
            return;
        }
        if (!partitionExists(table, FUTURE_PARTITION)) {
            log.warn("La tabla {} no tiene partición {} (¿no está particionada?); se omite", table, FUTURE_PARTITION);
            return;
        }

        String upperBound = (year + 1) + "-01-01";
        // table/year son constantes controladas (no input externo) -> formateo seguro.
        String sql = String.format(
                "ALTER TABLE %s REORGANIZE PARTITION %s INTO ("
                        + "PARTITION %s VALUES LESS THAN ('%s'), "
                        + "PARTITION %s VALUES LESS THAN (MAXVALUE))",
                table, FUTURE_PARTITION, partitionName, upperBound, FUTURE_PARTITION);

        log.info("Creando partición anual {} en {} (REORGANIZE {})", partitionName, table, FUTURE_PARTITION);
        jdbcTemplate.execute(sql);
        log.info("Partición {} creada en {}", partitionName, table);
    }

    private boolean partitionExists(String table, String partitionName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.PARTITIONS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND PARTITION_NAME = ?",
                Integer.class, table, partitionName);
        return count != null && count > 0;
    }
}
