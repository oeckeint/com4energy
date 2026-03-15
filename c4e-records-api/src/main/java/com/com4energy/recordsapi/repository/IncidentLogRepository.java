package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.domain.entity.IncidentLog;
import com.com4energy.recordsapi.domain.enums.IncidentSeverity;
import com.com4energy.recordsapi.domain.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentLogRepository extends JpaRepository<IncidentLog, Long> {

    List<IncidentLog> findBySeverity(IncidentSeverity severity);

    List<IncidentLog> findByStatus(IncidentStatus status);

    List<IncidentLog> findByServiceName(String serviceName);

    List<IncidentLog> findByStatusAndSeverity(IncidentStatus status, IncidentSeverity severity);

    List<IncidentLog> findTop20ByOrderByCreatedOnDesc();

    List<IncidentLog> findByEnvironment(String environment);

    List<IncidentLog> findByTraceId(String traceId);

    List<IncidentLog> findByStatusIn(List<IncidentStatus> statuses);

    Page<IncidentLog> findBySeverity(IncidentSeverity severity, Pageable pageable);

}
