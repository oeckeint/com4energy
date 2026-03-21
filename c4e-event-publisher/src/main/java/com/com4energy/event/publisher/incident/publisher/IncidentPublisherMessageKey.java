package com.com4energy.event.publisher.incident.publisher;

import com.com4energy.i18n.core.MessageKey;

public enum IncidentPublisherMessageKey implements MessageKey {

    INCIDENT_TYPE_NOT_CONFIGURED("incident.publisher.type.not.configured");

    private final String key;

    IncidentPublisherMessageKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

}
