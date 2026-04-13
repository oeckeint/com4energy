package com.com4energy.recordsapi.service;

import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordMetricsByTypeResponse;
import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordMetricsSummaryResponse;
import com.com4energy.recordsapi.repository.FileRecordMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileRecordMetricsService {

    private final FileRecordMetricsRepository repository;

    public FileRecordMetricsSummaryResponse summary(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String origin,
            String status,
            String fileType
    ) {
        return repository.fetchSummary(startDate, endDate, origin, status, fileType);
    }

    public List<FileRecordMetricsByTypeResponse> byType(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String origin,
            String status
    ) {
        return repository.fetchByType(startDate, endDate, origin, status);
    }
}

