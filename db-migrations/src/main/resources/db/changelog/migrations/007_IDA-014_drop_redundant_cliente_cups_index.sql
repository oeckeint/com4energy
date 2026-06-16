-- liquibase formatted sql

-- changeset jesus:IDA-014-007
-- comment: idx_cliente_cups es redundante con la constraint cups_UNIQUE (ambos indexan cups). Se elimina para ahorrar overhead de escritura/almacenamiento; la unique sigue sirviendo igualdad y LIKE 'prefijo%'.
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'cliente' AND index_name = 'idx_cliente_cups'

DROP INDEX idx_cliente_cups ON cliente;

-- rollback CREATE INDEX idx_cliente_cups ON cliente (cups);
