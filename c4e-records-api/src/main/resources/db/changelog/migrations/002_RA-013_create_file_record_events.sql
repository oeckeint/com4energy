-- liquibase formatted sql

-- changeset jesus:RA-013
-- context:dev,qa,prod
-- comment Crear tabla file_record_events para persistir eventos del ingestion-service

CREATE TABLE sge.file_record_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- ================================
    -- ORIGIN REFERENCE
    -- ================================
    source_id   VARCHAR(100)  COMMENT 'ID del registro en el ingestion-service',
    filename    VARCHAR(255)  NOT NULL,
    extension   VARCHAR(20),
    file_type   VARCHAR(50),
    final_path  VARCHAR(500),

    -- ================================
    -- STATUS
    -- ================================
    status      VARCHAR(30)   NOT NULL COMMENT 'PENDING, REJECTED, etc.',
    origin      VARCHAR(30)   COMMENT 'API, AUTOMATIC, etc.',

    -- ================================
    -- FAILURE DETAILS (solo para FILE_REJECTED)
    -- ================================
    failure_reason              VARCHAR(50),
    failure_reason_description  TEXT,
    failed_line_number          INT         NULL COMMENT 'Numero de linea que fallo, si aplica',
    failed_line_reference       TEXT        COMMENT 'Contenido o referencia de la linea que fallo',
    comment                     TEXT,
    created_by                  VARCHAR(100),

    -- ================================
    -- EVENT METADATA
    -- ================================
    event_type   VARCHAR(50)  NOT NULL COMMENT 'Tipo de evento recibido desde outbox, por ejemplo FILE_REJECTED',
    occurred_at  TIMESTAMP    NULL     COMMENT 'Cuando ocurrio el evento en ingestion-service',
    received_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Cuando lo recibio records-api',

    -- ================================
    -- PAYLOAD DATA (RA-014)
    -- ================================
    metadata_json TEXT         NULL     COMMENT 'Metadata adicional del payload del evento',
    raw_payload   LONGTEXT     NULL     COMMENT 'Payload completo recibido desde RabbitMQ'
);

-- ================================
-- INDEXES
-- ================================
CREATE INDEX idx_fre_source_id   ON sge.file_record_events (source_id);
CREATE INDEX idx_fre_status      ON sge.file_record_events (status);
CREATE INDEX idx_fre_event_type  ON sge.file_record_events (event_type);
CREATE INDEX idx_fre_received_at ON sge.file_record_events (received_at);

-- rollback DROP TABLE IF EXISTS sge.file_record_events;

