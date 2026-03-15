package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import com.com4energy.recordsapi.domain.entity.IncidentLog;
import com.com4energy.recordsapi.domain.enums.IncidentSeverity;
import com.com4energy.recordsapi.domain.enums.IncidentStatus;
import com.com4energy.recordsapi.exception.ResourceNotFoundException;
import com.com4energy.recordsapi.repository.IncidentLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentLogService {

    private final IncidentLogRepository repository;

    public IncidentLog registerNewIncident(IncidentLog incident) {
        incident.setStatus(IncidentStatus.NEW);
        return repository.save(incident);
    }

    public List<IncidentLog> getOpenIncidents() {
        return repository.findByStatusIn(List.of(IncidentStatus.NEW, IncidentStatus.IN_PROGRESS));
    }

    public Page<IncidentLog> getIncidentsBySeverity(IncidentSeverity severity, Pageable pageable) {
        return repository.findBySeverity(severity, pageable);
    }

    public List<IncidentLog> getIncidentsByService(String serviceName) {
        return repository.findByServiceName(serviceName);
    }

    public List<IncidentLog> getIncidentsByTraceId(String traceId) {
        return repository.findByTraceId(traceId);
    }

    public List<IncidentLog> getRecentIncidents() {
        return repository.findTop20ByOrderByCreatedOnDesc();
    }

    public IncidentLog updateIncidentStatus(Long id, IncidentStatus status) {
        IncidentLog incident = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Messages.format(MessageKey.INCIDENT_LOG_NOT_FOUND, id)));

        incident.setStatus(status);

        return repository.save(incident);
    }

}
