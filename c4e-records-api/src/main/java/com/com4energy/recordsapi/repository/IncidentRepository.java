package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.domain.entity.Incident;
import com.com4energy.incidents.shared.contract.IncidentSeverity;
import com.com4energy.incidents.shared.contract.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findBySeverity(IncidentSeverity severity);

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByServiceName(String serviceName);

    List<Incident> findByStatusAndSeverity(IncidentStatus status, IncidentSeverity severity);

    List<Incident> findTop20ByOrderByCreatedOnDesc();

    List<Incident> findByEnvironment(String environment);

    List<Incident> findByTraceId(String traceId);

    List<Incident> findByStatusIn(List<IncidentStatus> statuses);

    Page<Incident> findBySeverity(IncidentSeverity severity, Pageable pageable);

}
