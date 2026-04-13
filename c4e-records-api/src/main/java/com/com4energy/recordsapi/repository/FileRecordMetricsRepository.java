package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordMetricsByTypeResponse;
import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordMetricsSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FileRecordMetricsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FileRecordMetricsSummaryResponse fetchSummary(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String origin,
            String status,
            String fileType
    ) {
        SqlFilter filter = buildFilter(startDate, endDate, origin, status, fileType);

        String sql = """
                SELECT
                  COUNT(*) AS total_files,
                   COALESCE(SUM(fr.processed_records), 0) AS total_processed_records,
                   COALESCE(SUM(fr.defected_records), 0) AS total_defected_records,
                  COALESCE(AVG(fr.processing_duration_ms), 0) AS avg_processing_duration_ms,
                  COALESCE(MIN(fr.processing_duration_ms), 0) AS min_processing_duration_ms,
                  COALESCE(MAX(fr.processing_duration_ms), 0) AS max_processing_duration_ms
                FROM file_records fr
                WHERE 1 = 1
                """ + filter.whereClause;

        return jdbcTemplate.queryForObject(sql, filter.parameters, (rs, rowNum) -> FileRecordMetricsSummaryResponse.builder()
                .totalFiles(rs.getLong("total_files"))
                 .totalProcessedRecords(rs.getLong("total_processed_records"))
                 .totalDefectedRecords(rs.getLong("total_defected_records"))
                .avgProcessingDurationMs(rs.getLong("avg_processing_duration_ms"))
                .minProcessingDurationMs(rs.getLong("min_processing_duration_ms"))
                .maxProcessingDurationMs(rs.getLong("max_processing_duration_ms"))
                .build());
    }

    public List<FileRecordMetricsByTypeResponse> fetchByType(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String origin,
            String status
    ) {
        SqlFilter filter = buildFilter(startDate, endDate, origin, status, null);

        String sql = """
                SELECT
                  COALESCE(fr.type, 'UNKNOWN') AS file_type,
                  COUNT(*) AS total_files,
                   COALESCE(SUM(fr.processed_records), 0) AS total_processed_records,
                   COALESCE(SUM(fr.defected_records), 0) AS total_defected_records,
                  COALESCE(AVG(fr.processing_duration_ms), 0) AS avg_processing_duration_ms
                FROM file_records fr
                WHERE 1 = 1
                """ + filter.whereClause + """
                GROUP BY fr.type
                ORDER BY total_files DESC, file_type ASC
                """;

        return jdbcTemplate.query(sql, filter.parameters, (rs, rowNum) -> mapByType(rs));
    }

    private FileRecordMetricsByTypeResponse mapByType(ResultSet rs) throws SQLException {
        return FileRecordMetricsByTypeResponse.builder()
                .fileType(rs.getString("file_type"))
                .totalFiles(rs.getLong("total_files"))
                 .totalProcessedRecords(rs.getLong("total_processed_records"))
                 .totalDefectedRecords(rs.getLong("total_defected_records"))
                .avgProcessingDurationMs(rs.getLong("avg_processing_duration_ms"))
                .build();
    }

    private SqlFilter buildFilter(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String origin,
            String status,
            String fileType
    ) {
        List<String> clauses = new ArrayList<>();
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        if (startDate != null) {
            clauses.add("AND fr.processed_at >= :startDate");
            parameters.addValue("startDate", startDate);
        }

        if (endDate != null) {
            clauses.add("AND fr.processed_at <= :endDate");
            parameters.addValue("endDate", endDate);
        }

        if (origin != null && !origin.isBlank()) {
            clauses.add("AND fr.origin = :origin");
            parameters.addValue("origin", origin.trim());
        }

        if (status != null && !status.isBlank()) {
            clauses.add("AND fr.status = :status");
            parameters.addValue("status", status.trim());
        }

        if (fileType != null && !fileType.isBlank()) {
            clauses.add("AND fr.type = :fileType");
            parameters.addValue("fileType", fileType.trim());
        }

        return new SqlFilter(String.join("\n", clauses), parameters);
    }

    private record SqlFilter(String whereClause, MapSqlParameterSource parameters) {
    }
}

