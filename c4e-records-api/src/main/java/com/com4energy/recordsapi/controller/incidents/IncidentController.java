package com.com4energy.recordsapi.controller.incidents;

import com.com4energy.recordsapi.domain.entity.Incident;
import com.com4energy.recordsapi.domain.enums.IncidentSeverity;
import com.com4energy.recordsapi.domain.enums.IncidentStatus;
import com.com4energy.recordsapi.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService service;

    @PostMapping
    public Incident createIncident(@RequestBody Incident incident) {
        return service.registerNewIncident(incident);
    }

    @GetMapping("/open")
    public List<Incident> getOpenIncidents() {
        return service.getOpenIncidents();
    }

    @GetMapping("/recent")
    public List<Incident> getRecentIncidents() {
        return service.getRecentIncidents();
    }

    @GetMapping("/service/{serviceName}")
    public List<Incident> getByService(@PathVariable String serviceName) {
        return service.getIncidentsByService(serviceName);
    }

    @GetMapping("/trace/{traceId}")
    public List<Incident> getByTrace(@PathVariable String traceId) {
        return service.getIncidentsByTraceId(traceId);
    }

    @GetMapping("/severity/{severity}")
    public Page<Incident> getBySeverity(@PathVariable IncidentSeverity severity, Pageable pageable) {
        return service.getIncidentsBySeverity(severity, pageable);
    }

    @PatchMapping("/{id}/status")
    public Incident updateStatus(@PathVariable Long id, @RequestParam IncidentStatus status) {
        return service.updateIncidentStatus(id, status);
    }

}
