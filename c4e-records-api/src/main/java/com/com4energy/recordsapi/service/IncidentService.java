package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import com.com4energy.recordsapi.domain.entity.messaging.Incident;
import com.com4energy.event.publisher.incident.contract.IncidentSeverity;
import com.com4energy.event.publisher.incident.contract.IncidentStatus;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository repository;

    public Incident registerNewIncident(Incident incident) {
        incident.setStatus(IncidentStatus.NEW);
        return repository.save(incident);
    }

    public List<Incident> getOpenIncidents() {
        return repository.findByStatusIn(List.of(IncidentStatus.NEW, IncidentStatus.IN_PROGRESS));
    }

    public Page<Incident> getIncidentsBySeverity(IncidentSeverity severity, Pageable pageable) {
        return repository.findBySeverity(severity, pageable);
    }

    public List<Incident> getIncidentsByService(String serviceName) {
        return repository.findByServiceName(serviceName);
    }

    public List<Incident> getIncidentsByTraceId(String traceId) {
        return repository.findByTraceId(traceId);
    }

    public List<Incident> getRecentIncidents() {
        return repository.findTop20ByOrderByCreatedOnDesc();
    }

    public Incident updateIncidentStatus(Long id, IncidentStatus status) {
        Incident incident = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.format(MessageKey.INCIDENT_LOG_NOT_FOUND, id)));

        incident.setStatus(status);

        return repository.save(incident);
    }

}
