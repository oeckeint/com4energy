package com.com4energy.recordsapi.controller.incidents;

import com.com4energy.recordsapi.domain.entity.messaging.Incident;
import com.com4energy.event.publisher.incident.contract.IncidentSeverity;
import com.com4energy.event.publisher.incident.contract.IncidentStatus;
import com.com4energy.recordsapi.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(IncidentConstants.BASE_PATH)
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService service;

    @PostMapping
    public Incident createIncident(@RequestBody Incident incident) {
        return service.registerNewIncident(incident);
    }

    @GetMapping(IncidentConstants.OPEN_PATH)
    public List<Incident> getOpenIncidents() {
        return service.getOpenIncidents();
    }

    @GetMapping(IncidentConstants.RECENT_PATH)
    public List<Incident> getRecentIncidents() {
        return service.getRecentIncidents();
    }

    @GetMapping(IncidentConstants.BY_SERVICE_PATH)
    public List<Incident> getByService(@PathVariable String serviceName) {
        return service.getIncidentsByService(serviceName);
    }

    @GetMapping(IncidentConstants.BY_TRACE_PATH)
    public List<Incident> getByTrace(@PathVariable String traceId) {
        return service.getIncidentsByTraceId(traceId);
    }

    @GetMapping(IncidentConstants.BY_SEVERITY_PATH)
    public Page<Incident> getBySeverity(@PathVariable IncidentSeverity severity, Pageable pageable) {
        return service.getIncidentsBySeverity(severity, pageable);
    }

    @PatchMapping(IncidentConstants.UPDATE_STATUS_PATH)
    public Incident updateStatus(@PathVariable Long id, @RequestParam IncidentStatus status) {
        return service.updateIncidentStatus(id, status);
    }

}
