-- liquibase formatted sql

-- changeset jesus:IDA-014
-- comment Crear nuevas tablas de medidas para el ingestion-service basadas en medida_* pero con una estructura mas potente y flexible

-- Medida CCH
CREATE TABLE IF NOT EXISTS medida_cch (
    id_medida_cch BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    fecha DATETIME(3) NOT NULL,
    bandera_inv_ver INT NOT NULL,
    actent DECIMAL(12,4) NOT NULL,
    metod INT NULL,
    id_file_record BIGINT NOT NULL,
    source_family_key VARCHAR(64) NOT NULL COMMENT 'Identificador lógico común para todas las revisiones del mismo archivo',
    revision SMALLINT UNSIGNED NOT NULL COMMENT 'Revision incremental dentro de una familia de archivos',
    processing_iteration SMALLINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Número de iteración de procesamiento, inicia en 0 y se incrementa cada vez que el registro es procesado por el ingestion-service',
    payload_hash CHAR(64) NOT NULL,
    payload_hash_version SMALLINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by VARCHAR(64) NOT NULL,
    updated_at DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(64) NULL,

    PRIMARY KEY (id_medida_cch),

    UNIQUE KEY uk_medida_cch_business (id_cliente, fecha),
    INDEX idx_family_revision_medida_cch (source_family_key, revision),
    INDEX idx_id_file_record_medida_cch (id_file_record)
)
ENGINE=InnoDB
CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci
ROW_FORMAT=DYNAMIC;

-- rollback DROP TABLE IF EXISTS medida_cch;
