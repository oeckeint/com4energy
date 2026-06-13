package com.com4energy.outboxworker.outbox.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class OutboxEventRepository {

    private static final RowMapper<OutboxEventRecord> OUTBOX_ROW_MAPPER = new OutboxEventRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OutboxEventRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> findPendingIdsForUpdate(int batchSize) {
        String sql = """
                select id
                from outbox_event
                where status = 'PENDING'
                  and locked_at is null
                order by created_at asc
                limit :batchSize
                for update skip locked
                """;

        return jdbcTemplate.queryForList(sql, Map.of("batchSize", batchSize), Long.class);
    }

    public void markAsProcessing(List<Long> ids, String workerId) {
        if (ids.isEmpty()) {
            return;
        }

        String sql = """
                update outbox_event
                set status = 'PROCESSING',
                    locked_at = now(),
                    locked_by = :workerId
                where id in (:ids)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("workerId", workerId)
                .addValue("ids", ids);

        jdbcTemplate.update(sql, params);
    }

    public List<OutboxEventRecord> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        String sql = """
                select id, aggregate_type, aggregate_id, event_type, payload, status, retries, created_at
                from outbox_event
                where id in (:ids)
                order by created_at asc
                """;

        return jdbcTemplate.query(sql, Map.of("ids", ids), OUTBOX_ROW_MAPPER);
    }

    public void markProcessed(Long id) {
        String sql = """
                update outbox_event
                set status = 'PROCESSED',
                    processed_at = now(),
                    locked_at = null,
                    locked_by = null,
                    error_message = null
                where id = :id
                """;

        jdbcTemplate.update(sql, Map.of("id", id));
    }

    public int deleteProcessedOlderThan(int retentionDays, int batchSize) {
        String sql = """
                delete from outbox_event
                where id in (
                    select id from (
                        select id
                        from outbox_event
                        where status = 'PROCESSED'
                          and processed_at < now() - interval :days day
                        order by processed_at asc, id asc
                        limit :batchSize
                    ) t
                )
                """;

        return jdbcTemplate.update(sql, Map.of("days", retentionDays, "batchSize", batchSize));
    }

    public void markFailed(Long id, String errorMessage) {
        String sql = """
                update outbox_event
                set status = 'FAILED',
                    retries = retries + 1,
                    error_message = :errorMessage,
                    locked_at = null,
                    locked_by = null
                where id = :id
                """;

        jdbcTemplate.update(sql, Map.of(
                "id", id,
                "errorMessage", truncate(errorMessage)
        ));
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    public record OutboxEventRecord(
            Long id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            String status,
            Integer retries,
            LocalDateTime createdAt
    ) {
    }

    private static class OutboxEventRowMapper implements RowMapper<OutboxEventRecord> {

        @Override
        public OutboxEventRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp createdAt = rs.getTimestamp("created_at");
            return new OutboxEventRecord(
                    rs.getLong("id"),
                    rs.getString("aggregate_type"),
                    rs.getString("aggregate_id"),
                    rs.getString("event_type"),
                    rs.getString("payload"),
                    rs.getString("status"),
                    rs.getInt("retries"),
                    createdAt != null ? createdAt.toLocalDateTime() : null
            );
        }
    }
}

