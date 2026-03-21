package com.com4energy.event.publisher.incident.contract;

/** Criticidad del incidente. Mayor valor = mayor prioridad. */
public enum IncidentSeverity {
    CRITICAL(4),
    ERROR(3),
    WARN(2),
    INFO(1);

    private final int level;

    IncidentSeverity(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public boolean isAlertRequired() {
        return this == CRITICAL || this == ERROR;
    }
}

