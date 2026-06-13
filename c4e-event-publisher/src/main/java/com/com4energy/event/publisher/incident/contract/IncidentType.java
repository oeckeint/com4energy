package com.com4energy.event.publisher.incident.contract;

/** Selector de canal RabbitMQ para publicar incidentes. */
public enum IncidentType {

    VALIDATION("validation"),
    INTEGRATION("integration"),
    SYSTEM("system");

    private final String key;

    IncidentType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}

