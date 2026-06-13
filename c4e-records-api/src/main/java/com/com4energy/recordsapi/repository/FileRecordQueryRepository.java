package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordListItemResponse;
import com.com4energy.recordsapi.service.dto.FileRecordQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class FileRecordQueryRepository {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "fr.id",
            "createdAt", "fr.created_at",
            "uploadedAt", "fr.uploaded_at",
            "processedAt", "fr.processed_at",
            "status", "fr.status",
            "type", "fr.type",
            "finalFilename", "fr.final_filename"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public long count(FileRecordQueryCriteria criteria) {
        SqlFilter filter = buildFilter(
                criteria.startDate(),
                criteria.endDate(),
                criteria.origin(),
                criteria.status(),
                criteria.fileType(),
                criteria.filename()
        );

        String sql = """
                SELECT COUNT(*)
                FROM file_records fr
                WHERE 1 = 1
                """ + filter.whereClause;

        Long total = jdbcTemplate.queryForObject(sql, filter.parameters, Long.class);
        return total == null ? 0L : total;
    }

    public List<FileRecordListItemResponse> fetchPage(FileRecordQueryCriteria criteria) {
        SqlFilter filter = buildFilter(
                criteria.startDate(),
                criteria.endDate(),
                criteria.origin(),
                criteria.status(),
                criteria.fileType(),
                criteria.filename()
        );
        String safeSortBy = SORT_COLUMNS.getOrDefault(criteria.sortBy(), SORT_COLUMNS.get("createdAt"));
        String safeSortDir = "asc".equalsIgnoreCase(criteria.sortDir()) ? "ASC" : "DESC";

        filter.parameters.addValue("limit", criteria.size());
        filter.parameters.addValue("offset", criteria.page() * criteria.size());

        String sql = """
                SELECT
                    fr.id,
                    fr.original_filename,
                    fr.final_filename,
                    fr.type,
                    fr.status,
                    fr.quality_status,
                    fr.origin,
                    fr.retry_count,
                    fr.processed_records,
                    fr.defected_records,
                    fr.processing_duration_ms,
                    fr.uploaded_at,
                    fr.processed_at,
                    fr.created_at
                FROM file_records fr
                WHERE 1 = 1
                """ + filter.whereClause + "\n" +
                "ORDER BY " + safeSortBy + " " + safeSortDir + "\n" +
                "LIMIT :limit OFFSET :offset";

        return jdbcTemplate.query(sql, filter.parameters, (rs, rowNum) -> mapRecord(rs));
    }

    private FileRecordListItemResponse mapRecord(ResultSet rs) throws SQLException {
        return FileRecordListItemResponse.builder()
                .id(rs.getLong("id"))
                .originalFilename(rs.getString("original_filename"))
                .finalFilename(rs.getString("final_filename"))
                .type(rs.getString("type"))
                .status(rs.getString("status"))
                .qualityStatus(rs.getString("quality_status"))
                .origin(rs.getString("origin"))
                .retryCount(rs.getInt("retry_count"))
                .processedRecords((Integer) rs.getObject("processed_records"))
                .defectedRecords((Integer) rs.getObject("defected_records"))
                .processingDurationMs((Long) rs.getObject("processing_duration_ms"))
                .uploadedAt(toLocalDateTime(rs, "uploaded_at"))
                .processedAt(toLocalDateTime(rs, "processed_at"))
                .createdAt(toLocalDateTime(rs, "created_at"))
                .build();
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private SqlFilter buildFilter(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String origin,
            String status,
            String fileType,
            String filename
    ) {
        List<String> clauses = new ArrayList<>();
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        if (startDate != null) {
            clauses.add("AND fr.created_at >= :startDate");
            parameters.addValue("startDate", startDate);
        }
        if (endDate != null) {
            clauses.add("AND fr.created_at <= :endDate");
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
        if (filename != null && !filename.isBlank()) {
            clauses.add("AND (fr.final_filename LIKE :filename OR fr.original_filename LIKE :filename)");
            parameters.addValue("filename", "%" + filename.trim() + "%");
        }

        return new SqlFilter(String.join("\n", clauses), parameters);
    }

    private record SqlFilter(String whereClause, MapSqlParameterSource parameters) {
    }
}


