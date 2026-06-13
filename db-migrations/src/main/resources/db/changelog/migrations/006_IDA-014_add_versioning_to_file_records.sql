-- liquibase formatted sql

-- changeset jesus:IDA-014-006
-- comment: Centralizar aquí evita duplicarlos en cada fila de medida_h/qh
-- y permite el pre-chequeo de familia en una sola query antes de procesar el archivo.

ALTER TABLE file_records
    ADD COLUMN source_family_key  VARCHAR(64)      NULL COMMENT 'Nombre del archivo sin extensión — agrupa todas las revisiones e iteraciones del mismo origen',
    ADD COLUMN revision           SMALLINT UNSIGNED NULL COMMENT 'Primera extensión del archivo (ej. .3 → revision=3); gana la revisión más alta',
    ADD COLUMN processing_iteration SMALLINT UNSIGNED NULL DEFAULT 0 COMMENT 'Segunda extensión del archivo cuando existe (ej. .3.1 → iteration=1); gana la iteración más alta dentro de la misma revisión',
    ADD INDEX  idx_file_records_family_version (source_family_key, revision, processing_iteration, status);

-- rollback ALTER TABLE file_records
-- rollback     DROP INDEX  idx_file_records_family_version,
-- rollback     DROP COLUMN processing_iteration,
-- rollback     DROP COLUMN revision,
-- rollback     DROP COLUMN source_family_key;
