-- liquibase formatted sql

-- changeset jesus:IDA-014

-- Medida QH
CREATE TABLE IF NOT EXISTS medida_qh (
    id_medida_qh BIGINT UNSIGNED NOT NULL COMMENT 'TSID generado por la aplicación (no AUTO_INCREMENT) para permitir JDBC batching',
    id_cliente INT NOT NULL,
    tipo_medida TINYINT UNSIGNED NOT NULL COMMENT 'Clasificación técnica del archivo (valores < 100)',
    fecha DATETIME NOT NULL COMMENT 'Granularidad a segundos: las medidas caen siempre en minuto exacto',
    bandera_inv_ver BOOLEAN NOT NULL COMMENT 'Booleano (MySQL lo almacena como TINYINT(1): 0/1)',
    actent SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud (valores < 1k; zona segura ~10k, tope 65535)',
    qactent SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad (valores < 1k)',
    actsal SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud (valores < 1k; zona segura ~10k, tope 65535)',
    qactsal SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad (valores < 1k)',
    r_q1 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud (valores < 1k; zona segura ~10k, tope 65535)',
    qr_q1 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad (valores < 1k)',
    r_q2 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud (valores < 1k; zona segura ~10k, tope 65535)',
    qr_q2 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad (valores < 1k)',
    r_q3 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud (valores < 1k; zona segura ~10k, tope 65535)',
    qr_q3 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad (valores < 1k)',
    r_q4 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud (valores < 1k; zona segura ~10k, tope 65535)',
    qr_q4 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad (valores < 1k)',
    medres1 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud (valores < 1k; zona segura ~10k, tope 65535)',
    qmedres1 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad (valores < 1k)',
    medres2 SMALLINT UNSIGNED NOT NULL COMMENT 'Magnitud (valores < 1k; zona segura ~10k, tope 65535)',
    qmedres2 SMALLINT UNSIGNED NOT NULL COMMENT 'Código de calidad (valores < 1k)',
    metod_obt TINYINT UNSIGNED NULL COMMENT 'Método de obtención (valores < 100)',
    id_file_record BIGINT NOT NULL COMMENT 'FK al file_record; de aquí se deriva creación/origen/versión (sin columnas de auditoría propias)',
    payload_hash BINARY(8) NOT NULL COMMENT 'SHA-256 truncado a 8 bytes; change-detection 1-vs-1 por (id_cliente, fecha), no es hash de seguridad',
    payload_hash_version SMALLINT NOT NULL DEFAULT 1,

    -- 'fecha' va en la PK porque MySQL exige la columna de partición en toda clave única/PK.
    -- El @Id de la entidad sigue siendo solo id_medida_qh (la PK compuesta es físico).
    PRIMARY KEY (id_medida_qh, fecha),

    UNIQUE KEY uk_medida_qh_business (id_cliente, fecha),
    INDEX idx_id_file_record_medida_qh (id_file_record)
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

-- rollback DROP TABLE IF EXISTS medida_qh;
