-- liquibase formatted sql

-- changeset jesus:RA-012
-- context:dev,qa,prod
-- comment Crear DB y flujo para persistencia del registro de incidentes

CREATE TABLE sge.incidents_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- ================================
    -- SERVICE INFORMATION
    -- ================================
    service_name VARCHAR(100) NOT NULL,
    environment ENUM('DEV','QA','PROD'),

    -- ================================
    -- EXECUTION CONTEXT
    -- ================================
    endpoint VARCHAR(150),
    method_name VARCHAR(150),
    http_method VARCHAR(10),

    -- ================================
    -- DISTRIBUTED TRACING
    -- ================================
    trace_id VARCHAR(100),
    span_id VARCHAR(100),

    -- ================================
    -- USER CONTEXT
    -- ================================
    user_id VARCHAR(50),

    -- ================================
    -- ERROR INFORMATION
    -- ================================
    exception_type VARCHAR(150) NOT NULL,
    message TEXT,
    stack_trace TEXT,

    -- ================================
    -- INCIDENT CLASSIFICATION
    -- ================================
    category VARCHAR(30) COMMENT 'APPLICATION, FILE_PROCESSING, INTEGRATION, VALIDATION, SECURITY, SYSTEM',
    severity VARCHAR(20) COMMENT 'CRITICAL, ERROR, WARN, INFO',
    status VARCHAR(20) DEFAULT 'NEW' COMMENT 'NEW, IN_PROGRESS, SOLVED, DISCARDED',
    error_code VARCHAR(50),

    -- ================================
    -- FILE CONTEXT (optional)
    -- ================================
    filename VARCHAR(255),
    file_type VARCHAR(50),
    folder_name VARCHAR(50),

    -- ================================
    -- FLEXIBLE DATA
    -- ================================
    metadata JSON,

    -- ================================
    -- AUDIT
    -- ================================
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

-- ================================
-- INDEXES
-- ================================

CREATE INDEX idx_incident_service ON incidents_log(service_name);
CREATE INDEX idx_incident_category ON incidents_log(category);
CREATE INDEX idx_incident_type ON incidents_log(exception_type);
CREATE INDEX idx_incident_created_on ON incidents_log(created_on);
CREATE INDEX idx_incident_trace ON incidents_log(trace_id);
CREATE INDEX idx_incident_filename ON incidents_log(filename);
CREATE INDEX idx_incident_service_created ON incidents_log(service_name, created_on);

-- índice compuesto muy útil para monitoreo
CREATE INDEX idx_incident_severity_status ON incidents_log(severity, status);

-- rollback DROP TABLE IF EXISTS incidents_log;