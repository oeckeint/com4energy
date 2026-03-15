package com.com4energy.recordsapi.domain.enums;

public enum IncidentCategory {

    APPLICATION, // errores del API o lógica errores del API o lógica
    FILE_PROCESSING, // parsing, formatos, pipelines
    INTEGRATION, // APIs externas, colas, servicios
    VALIDATION, // reglas de negocio
    SECURITY, // autenticación, permisos
    SYSTEM; // infraestructura

}