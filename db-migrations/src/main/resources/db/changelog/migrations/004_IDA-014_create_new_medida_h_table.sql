-- liquibase formatted sql

-- changeset jesus:IDA-014

-- Medida H
CREATE TABLE IF NOT EXISTS medida_h (
    id_medida_h BIGINT UNSIGNED NOT NULL COMMENT 'TSID generado por la aplicación (no AUTO_INCREMENT) para permitir JDBC batching',
    id_cliente INT NOT NULL,
    tipo_medida INT NOT NULL COMMENT 'Clasificación técnica derivada del archivo de ingesta',
    fecha DATETIME(3) NOT NULL,
    bandera_inv_ver INT NOT NULL ,
    actent INT UNSIGNED NOT NULL,
    qactent INT UNSIGNED NOT NULL,
    actsal INT UNSIGNED NOT NULL,
    qactsal INT UNSIGNED NOT NULL,
    r_q1 INT UNSIGNED NOT NULL,
    qr_q1 INT UNSIGNED NOT NULL,
    r_q2 INT UNSIGNED NOT NULL,
    qr_q2 INT UNSIGNED NOT NULL,
    r_q3 INT UNSIGNED NOT NULL,
    qr_q3 INT UNSIGNED NOT NULL,
    r_q4 INT UNSIGNED NOT NULL,
    qr_q4 INT UNSIGNED NOT NULL,
    medres1 INT UNSIGNED NOT NULL,
    qmedres1 INT UNSIGNED NOT NULL,
    medres2 INT UNSIGNED NOT NULL,
    qmedres2 INT UNSIGNED NOT NULL,
    metod_obt INT NULL,
    id_file_record BIGINT NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    payload_hash_version SMALLINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by VARCHAR(64) NOT NULL,
    updated_at DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(64) NULL,

    PRIMARY KEY (id_medida_h),

    UNIQUE KEY uk_medida_h_business (id_cliente, fecha),
    INDEX idx_id_file_record_medida_h (id_file_record)
)
ENGINE=InnoDB
CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci
ROW_FORMAT=DYNAMIC;

-- rollback DROP TABLE IF EXISTS medida_h;
