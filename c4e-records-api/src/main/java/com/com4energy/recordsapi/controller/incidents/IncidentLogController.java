package com.com4energy.recordsapi.controller.incidents;

import com.com4energy.recordsapi.domain.entity.IncidentLog;
import com.com4energy.recordsapi.domain.enums.IncidentSeverity;
import com.com4energy.recordsapi.domain.enums.IncidentStatus;
import com.com4energy.recordsapi.service.IncidentLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentLogController {

    private final IncidentLogService service;

    @PostMapping
    public IncidentLog createIncident(@RequestBody IncidentLog incident) {
        return service.registerNewIncident(incident);
    }

    @GetMapping("/open")
    public List<IncidentLog> getOpenIncidents() {
        return service.getOpenIncidents();
    }

    @GetMapping("/recent")
    public List<IncidentLog> getRecentIncidents() {
        return service.getRecentIncidents();
    }

    @GetMapping("/service/{serviceName}")
    public List<IncidentLog> getByService(@PathVariable String serviceName) {
        return service.getIncidentsByService(serviceName);
    }

    @GetMapping("/trace/{traceId}")
    public List<IncidentLog> getByTrace(@PathVariable String traceId) {
        return service.getIncidentsByTraceId(traceId);
    }

    @GetMapping("/severity/{severity}")
    public Page<IncidentLog> getBySeverity(@PathVariable IncidentSeverity severity, Pageable pageable) {
        return service.getIncidentsBySeverity(severity, pageable);
    }

    @PatchMapping("/{id}/status")
    public IncidentLog updateStatus(@PathVariable Long id, @RequestParam IncidentStatus status) {
        return service.updateIncidentStatus(id, status);
    }

}
