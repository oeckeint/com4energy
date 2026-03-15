-- liquibase formatted sql

-- changeset jesus:RA-012-seed context:dev,qa
-- comment Seed inicial de datos de prueba para incidents
-- precondition-table-exists table:incidents schema:sge

INSERT INTO sge.incidents (
    service_name,
    environment,
    endpoint,
    method_name,
    http_method,
    trace_id,
    span_id,
    user_id,
    exception_type,
    message,
    stack_trace,
    category,
    severity,
    error_code,
    filename,
    file_type,
    folder_name,
    status,
    metadata,
    created_by
) VALUES

      (
          'c4e-files-api',
          'dev',
          '/upload',
          'processCsv',
          'POST',
          'trace-001',
          'span-001',
          'user123',
          'ValidationException',
          'Columna obligatoria email no encontrada',
          'stack trace ejemplo...',
          'VALIDATION',
          'ERROR',
          'CSV_COLUMN_MISSING',
          'clientes_enero.csv',
          'csv',
          'error',
          'NEW',
          '{"seed": true, "row": 14, "column": "email"}',
          'system'
      ),

      (
          'c4e-files-api',
          'dev',
          '/upload',
          'processPdf',
          'POST',
          'trace-002',
          'span-002',
          'user456',
          'IOException',
          'No se pudo leer el archivo PDF',
          'stack trace ejemplo...',
          'FILE_PROCESSING',
          'CRITICAL',
          'PDF_READ_ERROR',
          'contrato_marzo.pdf',
          'pdf',
          'error',
          'IN_PROGRESS',
          '{"seed": true, "fileSize": "2MB"}',
          'system'
      ),

      (
          'c4e-payments-api',
          'qa',
          '/process',
          'validatePaymentFile',
          'POST',
          'trace-003',
          'span-003',
          NULL,
          'ValidationWarning',
          'Formato inesperado en columna amount',
          'stack trace ejemplo...',
          'VALIDATION',
          'WARN',
          'CSV_FORMAT_WARNING',
          'pagos_enero.csv',
          'csv',
          'pending',
          'NEW',
          '{"seed": true, "column": "amount"}',
          'system'
      ),

      (
          'c4e-files-api',
          'prod',
          '/upload',
          'processCsv',
          'POST',
          'trace-004',
          'span-004',
          'user999',
          'NullPointerException',
          'Valor nulo inesperado en procesamiento',
          'stack trace ejemplo...',
          'FILE_PROCESSING',
          'ERROR',
          'NULL_VALUE_PROCESSING',
          'clientes_febrero.csv',
          'csv',
          'processed',
          'SOLVED',
          '{"seed": true, "row": 22}',
          'system'
      );

-- rollback DELETE FROM sge.incidents WHERE created_by='system';