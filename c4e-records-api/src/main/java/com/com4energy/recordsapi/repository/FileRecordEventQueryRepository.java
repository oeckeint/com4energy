package com.com4energy.recordsapi.repository;

import com.com4energy.recordsapi.controller.filerecords.dto.FileRecordEventListItemResponse;
import com.com4energy.recordsapi.service.dto.FileRecordEventQueryCriteria;
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
public class FileRecordEventQueryRepository {

    private static final String DEFAULT_SORT = "receivedAt";

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "fre.id",
            "receivedAt", "fre.received_at",
            "occurredAt", "fre.occurred_at",
            "eventType", "fre.event_type",
            "status", "fre.status",
            "fileType", "fre.file_type",
            "filename", "fre.filename"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public long count(FileRecordEventQueryCriteria criteria) {
        SqlFilter filter = buildFilter(
                criteria.startDate(),
                criteria.endDate(),
                criteria.eventType(),
                criteria.status(),
                criteria.origin(),
                criteria.fileType(),
                criteria.filename()
        );

        String sql = """
                SELECT COUNT(*)
                FROM file_record_events fre
                WHERE 1 = 1
                """ + filter.whereClause;

        Long total = jdbcTemplate.queryForObject(sql, filter.parameters, Long.class);
        return total == null ? 0L : total;
    }

    public List<FileRecordEventListItemResponse> fetchPage(FileRecordEventQueryCriteria criteria) {
        SqlFilter filter = buildFilter(
                criteria.startDate(),
                criteria.endDate(),
                criteria.eventType(),
                criteria.status(),
                criteria.origin(),
                criteria.fileType(),
                criteria.filename()
        );

        String safeSortBy = SORT_COLUMNS.getOrDefault(criteria.sortBy(), SORT_COLUMNS.get(DEFAULT_SORT));
        String safeSortDir = "asc".equalsIgnoreCase(criteria.sortDir()) ? "ASC" : "DESC";

        filter.parameters.addValue("limit", criteria.size());
        filter.parameters.addValue("offset", criteria.page() * criteria.size());

        String sql = """
                SELECT
                    fre.id,
                    fre.source_id,
                    fre.event_type,
                    fre.status,
                    fre.filename,
                    fre.file_type,
                    fre.origin,
                    fre.failure_reason,
                    fre.failure_reason_description,
                    fre.failed_line_number,
                    fre.created_by,
                    fre.occurred_at,
                    fre.received_at,
                    fre.metadata_json
                FROM file_record_events fre
                WHERE 1 = 1
                """ + filter.whereClause + "\n" +
                "ORDER BY " + safeSortBy + " " + safeSortDir + "\n" +
                "LIMIT :limit OFFSET :offset";

        return jdbcTemplate.query(sql, filter.parameters, (rs, rowNum) -> mapItem(rs));
    }

    private FileRecordEventListItemResponse mapItem(ResultSet rs) throws SQLException {
        return FileRecordEventListItemResponse.builder()
                .id(rs.getLong("id"))
                .sourceId(rs.getString("source_id"))
                .eventType(rs.getString("event_type"))
                .status(rs.getString("status"))
                .filename(rs.getString("filename"))
                .fileType(rs.getString("file_type"))
                .origin(rs.getString("origin"))
                .failureReason(rs.getString("failure_reason"))
                .failureReasonDescription(rs.getString("failure_reason_description"))
                .failedLineNumber((Integer) rs.getObject("failed_line_number"))
                .createdBy(rs.getString("created_by"))
                .occurredAt(toLocalDateTime(rs, "occurred_at"))
                .receivedAt(toLocalDateTime(rs, "received_at"))
                .metadataJson(rs.getString("metadata_json"))
                .build();
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        var ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    private SqlFilter buildFilter(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String eventType,
            String status,
            String origin,
            String fileType,
            String filename
    ) {
        List<String> clauses = new ArrayList<>();
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        if (startDate != null) {
            clauses.add("AND fre.received_at >= :startDate");
            parameters.addValue("startDate", startDate);
        }
        if (endDate != null) {
            clauses.add("AND fre.received_at <= :endDate");
            parameters.addValue("endDate", endDate);
        }
        if (eventType != null && !eventType.isBlank()) {
            clauses.add("AND fre.event_type = :eventType");
            parameters.addValue("eventType", eventType.trim());
        }
        if (status != null && !status.isBlank()) {
            clauses.add("AND fre.status = :status");
            parameters.addValue("status", status.trim());
        }
        if (origin != null && !origin.isBlank()) {
            clauses.add("AND fre.origin = :origin");
            parameters.addValue("origin", origin.trim());
        }
        if (fileType != null && !fileType.isBlank()) {
            clauses.add("AND fre.file_type = :fileType");
            parameters.addValue("fileType", fileType.trim());
        }
        if (filename != null && !filename.isBlank()) {
            clauses.add("AND fre.filename LIKE :filename");
            parameters.addValue("filename", "%" + filename.trim() + "%");
        }

        return new SqlFilter(String.join("\n", clauses), parameters);
    }

    private record SqlFilter(String whereClause, MapSqlParameterSource parameters) {
    }
}

