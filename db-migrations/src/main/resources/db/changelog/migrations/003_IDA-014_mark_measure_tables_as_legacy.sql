-- liquibase formatted sql

-- changeset jesus:IDA-014
-- comment Marcar tablas de medidas como legacy para evitar que sean usadas por el ingestion-service

RENAME TABLE
    medida_h TO medida_h_legacy,
    medidaqh TO medida_qh_legacy,
    medida_cch TO medida_cch_legacy;

-- rollback
-- RENAME TABLE
--     medida_h_legacy TO medida_h,
--     medida_qh_legacy TO medidaqh,
--     medida_cch_legacy TO medida_cch;