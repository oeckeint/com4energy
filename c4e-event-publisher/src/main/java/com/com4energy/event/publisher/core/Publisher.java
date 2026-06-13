package com.com4energy.event.publisher.core;

import com.com4energy.event.publisher.incident.contract.IncidentEvent;
import com.com4energy.event.publisher.incident.contract.IncidentType;

public interface Publisher {

    void send(IncidentType type, IncidentEvent event);
}

