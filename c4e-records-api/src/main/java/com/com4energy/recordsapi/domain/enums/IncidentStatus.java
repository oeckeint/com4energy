package com.com4energy.recordsapi.domain.enums;

public enum IncidentStatus {

    NEW,
    IN_PROGRESS,
    SOLVED,
    DISCARDED;

    public boolean isFinalState() {
        return this == SOLVED || this == DISCARDED;
    }

}
