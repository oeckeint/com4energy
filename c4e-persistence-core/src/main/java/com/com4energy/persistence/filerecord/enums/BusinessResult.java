package com.com4energy.persistence.filerecord.enums;

public enum BusinessResult {
    NOT_PROCESSED,
    FULLY_SUCCEEDED,
    PARTIAL_SUCCEEDED,
    NOT_PERSISTED,
    // El archivo se procesó completo pero quedó obsoleto: todas sus filas se omitieron porque ya
    // existían registros de una revisión/iteración igual o más reciente (cero escrituras netas).
    SUPERSEDED
}
