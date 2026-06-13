-- liquibase formatted sql

-- changeset jesus:COW-001
-- context:dev,qa,prod
-- comment Crear tabla outbox_event con soporte para concurrencia (Outbox Pattern)

CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100),

    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retries INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL DEFAULT NULL,

    -- control de errores
    error_message TEXT NULL,

    -- locking para concurrencia
    locked_at TIMESTAMP NULL DEFAULT NULL,
    locked_by VARCHAR(100) NULL,

    -- indices criticos para performance
    INDEX idx_claim_pending (status, locked_at, created_at),
    INDEX idx_cleanup_processed (status, processed_at),
    INDEX idx_aggregate (aggregate_type, aggregate_id)
);

-- rollback DROP TABLE IF EXISTS outbox_event;

