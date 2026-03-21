package com.com4energy.event.publisher.incident.contract;

/** Estado del incidente dentro de su ciclo de vida. */
public enum IncidentStatus {
    NEW,
    IN_PROGRESS,
    SOLVED,
    DISCARDED;

    public boolean isFinalState() {
        return this == SOLVED || this == DISCARDED;
    }
}

