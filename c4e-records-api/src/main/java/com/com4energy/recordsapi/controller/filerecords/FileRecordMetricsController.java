package com.com4energy.recordsapi.controller.filerecords;

import com.com4energy.recordsapi.controller.common.ResponseHelper;
import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordMetricsByTypeResponse;
import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordMetricsSummaryResponse;
import com.com4energy.recordsapi.controller.medidas.DateRangeHelper;
import com.com4energy.recordsapi.service.FileRecordMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(FileRecordMetricsConstants.BASE_PATH)
@RequiredArgsConstructor
public class FileRecordMetricsController {

    private final FileRecordMetricsService service;

    //curl "http://localhost:8082/api/v1/file-records/metrics/summary?startDate=2026-04-01&endDate=2026-04-11&status=SUCCEEDED&origin=JOB"
   // curl "http://localhost:8082/api/v1/file-records/metrics/by-type?startDate=2026-04-01&endDate=2026-04-11&status=SUCCEEDED"


    @GetMapping(FileRecordMetricsConstants.SUMMARY_PATH)
    public ResponseEntity<FileRecordMetricsSummaryResponse> summary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fileType
    ) {
        LocalDateTime start = DateRangeHelper.parseDate(startDate, false);
        LocalDateTime end = DateRangeHelper.parseDate(endDate, true);

        return ResponseHelper.ok(service.summary(start, end, origin, status, fileType));
    }

    @GetMapping(FileRecordMetricsConstants.BY_TYPE_PATH)
    public ResponseEntity<List<FileRecordMetricsByTypeResponse>> byType(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String status
    ) {
        LocalDateTime start = DateRangeHelper.parseDate(startDate, false);
        LocalDateTime end = DateRangeHelper.parseDate(endDate, true);

        return ResponseHelper.ok(service.byType(start, end, origin, status));
    }
}

