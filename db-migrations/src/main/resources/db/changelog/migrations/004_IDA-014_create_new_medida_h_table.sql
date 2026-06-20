-- liquibase formatted sql

-- changeset jesus:IDA-014

-- Medida H
CREATE TABLE IF NOT EXISTS medida_h (
    id_medida_h BIGINT UNSIGNED NOT NULL COMMENT 'TSID app (no AUTO_INCREMENT, habilita JDBC batching). Rango BIGINT UNSIGNED: 0 .. 18,446,744,073,709,551,615',
    id_cliente INT NOT NULL COMMENT 'Rango INT (signed): -2,147,483,648 .. 2,147,483,647',
    tipo_medida TINYINT UNSIGNED NOT NULL COMMENT 'Clasificación técnica del archivo. Rango TINYINT UNSIGNED: 0 .. 255. Esperado: < 100',
    fecha DATETIME NOT NULL COMMENT 'Medidas en minuto exacto. Rango DATETIME: 1000-01-01 00:00:00 .. 9999-12-31 23:59:59',
    bandera_inv_ver BOOLEAN NOT NULL COMMENT 'Booleano (TINYINT(1)). Valores: 0 / 1',
    actent SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud energética (redondeo HALF_EVEN). Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k (zona segura ~10k)',
    qactent SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad. Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k',
    actsal SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud energética (redondeo HALF_EVEN). Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k (zona segura ~10k)',
    qactsal SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad. Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k',
    r_q1 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud energética (redondeo HALF_EVEN). Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k (zona segura ~10k)',
    qr_q1 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad. Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k',
    r_q2 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud energética (redondeo HALF_EVEN). Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k (zona segura ~10k)',
    qr_q2 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad. Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k',
    r_q3 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud energética (redondeo HALF_EVEN). Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k (zona segura ~10k)',
    qr_q3 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad. Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k',
    r_q4 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud energética (redondeo HALF_EVEN). Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k (zona segura ~10k)',
    qr_q4 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad. Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k',
    medres1 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud energética (redondeo HALF_EVEN). Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k (zona segura ~10k)',
    qmedres1 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad. Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k',
    medres2 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud energética (redondeo HALF_EVEN). Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k (zona segura ~10k)',
    qmedres2 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad. Rango SMALLINT UNSIGNED: 0 .. 65,535. Esperado: < 1k',
    metod_obt TINYINT UNSIGNED NULL COMMENT 'Método de obtención (nullable). Rango TINYINT UNSIGNED: 0 .. 255. Esperado: < 100',
    id_file_record BIGINT NOT NULL COMMENT 'FK a file_record (deriva creación/origen/versión). Rango BIGINT (signed): -9.22e18 .. 9.22e18',
    payload_hash BINARY(8) NOT NULL COMMENT 'SHA-256 truncado a 8 bytes (BINARY(8) = 8 bytes fijos); change-detection 1-vs-1, no de seguridad',
    payload_hash_version SMALLINT NOT NULL DEFAULT 1 COMMENT 'Versión del algoritmo de hash. Rango SMALLINT (signed): -32,768 .. 32,767',

    -- 'fecha' va en la PK porque MySQL exige la columna de partición en toda clave única/PK.
    -- El @Id de la entidad sigue siendo solo id_medida_h (la PK compuesta es físico).
    PRIMARY KEY (id_medida_h, fecha),

    UNIQUE KEY uk_medida_h_business (id_cliente, fecha),
    INDEX idx_id_file_record_medida_h (id_file_record)
)
ENGINE=InnoDB
CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci
ROW_FORMAT=DYNAMIC
-- Partición anual por fecha: respaldos por año y DROP PARTITION seguro para retención.
-- ghost-flows mantiene esto: reorganiza pfuture para crear el año siguiente antes de que llegue su data.
PARTITION BY RANGE COLUMNS (fecha) (
    PARTITION p2023 VALUES LESS THAN ('2024-01-01'),
    PARTITION p2024 VALUES LESS THAN ('2025-01-01'),
    PARTITION p2025 VALUES LESS THAN ('2026-01-01'),
    PARTITION p2026 VALUES LESS THAN ('2027-01-01'),
    PARTITION p2027 VALUES LESS THAN ('2028-01-01'),
    PARTITION pfuture VALUES LESS THAN (MAXVALUE)
);

-- rollback DROP TABLE IF EXISTS medida_h;
