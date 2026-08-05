package com.neueda.tms.repository.alert;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-based repository for Alert entities.
 * All queries use parameterized statements to prevent SQL injection.
 */
@Repository
public class AlertRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AlertRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── RowMapper ──────────────────────────────────────────────────────────────

    /**
     * Maps a flat alert row; transaction and rule are loaded by ID references.
     * Full joins are handled in service layer via separate repository calls to avoid N+1.
     */
    private static final RowMapper<Alert> ALERT_ROW_MAPPER = (rs, rowNum) -> {
        Alert a = new Alert();
        a.setId(rs.getLong("id"));
        a.setStatus(Alert.AlertStatus.valueOf(rs.getString("status")));
        a.setSeverity(Alert.AlertSeverity.valueOf(rs.getString("severity")));
        a.setDescription(rs.getString("description"));
        a.setAssignedTo(rs.getString("assigned_to"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        a.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        a.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        Transaction tx = new Transaction();
        tx.setId(rs.getLong("transaction_id"));
        tx.setTransactionRef(rs.getString("transaction_ref"));
        tx.setAccountId(rs.getString("account_id"));
        tx.setCustomerName(rs.getString("customer_name"));
        a.setTransaction(tx);

        MonitoringRule rule = new MonitoringRule();
        rule.setId(rs.getLong("rule_id"));
        rule.setRuleCode(rs.getString("rule_code"));
        rule.setRuleName(rs.getString("rule_name"));
        a.setRule(rule);

        return a;
    };

    // ── Write Operations ───────────────────────────────────────────────────────

    public Alert save(Alert alert) {
        if (alert.getId() == null) {
            return insert(alert);
        } else {
            update(alert);
            return alert;
        }
    }

    private Alert insert(Alert alert) {
        String sql = """
                INSERT INTO alerts (transaction_id, rule_id, status, severity, description, assigned_to, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, alert.getTransaction().getId());
            ps.setLong(2, alert.getRule().getId());
            ps.setString(3, alert.getStatus() != null
                    ? alert.getStatus().name() : Alert.AlertStatus.OPEN.name());
            ps.setString(4, alert.getSeverity().name());
            ps.setString(5, alert.getDescription());
            ps.setString(6, alert.getAssignedTo());
            ps.setTimestamp(7, Timestamp.valueOf(
                    alert.getCreatedAt() != null ? alert.getCreatedAt() : LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) alert.setId(key.longValue());
        return alert;
    }

    private void update(Alert alert) {
        String sql = """
                UPDATE alerts SET status = ?, assigned_to = ?, updated_at = ? WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                alert.getStatus().name(),
                alert.getAssignedTo(),
                alert.getUpdatedAt() != null ? Timestamp.valueOf(alert.getUpdatedAt()) : null,
                alert.getId());
    }

    // ── Read Operations ────────────────────────────────────────────────────────

    public Optional<Alert> findById(Long id) {
        String sql = """
                SELECT a.*, t.transaction_ref, t.account_id, t.customer_name,
                       r.rule_code, r.rule_name
                FROM alerts a
                JOIN transactions t ON a.transaction_id = t.id
                JOIN monitoring_rules r ON a.rule_id = r.id
                WHERE a.id = ?
                """;
        try {
            Alert a = jdbcTemplate.queryForObject(sql, ALERT_ROW_MAPPER, id);
            return Optional.ofNullable(a);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Alert> findByTransactionId(Long transactionId) {
        String sql = """
                SELECT a.*, t.transaction_ref, t.account_id, t.customer_name,
                       r.rule_code, r.rule_name
                FROM alerts a
                JOIN transactions t ON a.transaction_id = t.id
                JOIN monitoring_rules r ON a.rule_id = r.id
                WHERE a.transaction_id = ?
                """;
        return jdbcTemplate.query(sql, ALERT_ROW_MAPPER, transactionId);
    }

    public List<Alert> findByAccountId(String accountId) {
        String sql = """
                SELECT a.*, t.transaction_ref, t.account_id, t.customer_name,
                       r.rule_code, r.rule_name
                FROM alerts a
                JOIN transactions t ON a.transaction_id = t.id
                JOIN monitoring_rules r ON a.rule_id = r.id
                WHERE t.account_id = ?
                ORDER BY a.created_at DESC
                """;
        return jdbcTemplate.query(sql, ALERT_ROW_MAPPER, accountId);
    }

    /**
     * Dynamic paginated alert search with optional filters.
     * Uses parameterized ? placeholders throughout.
     */
    public List<Alert> searchAlerts(
            Alert.AlertStatus status, Alert.AlertSeverity severity,
            LocalDateTime fromDate, LocalDateTime toDate,
            String search, int offset, int limit,
            String sortBy, String sortDir) {

        String safeSort = allowedSortColumn(sortBy);
        String safeDir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        StringBuilder sql = new StringBuilder("""
                SELECT a.*, t.transaction_ref, t.account_id, t.customer_name,
                       r.rule_code, r.rule_name
                FROM alerts a
                JOIN transactions t ON a.transaction_id = t.id
                JOIN monitoring_rules r ON a.rule_id = r.id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, status, severity, fromDate, toDate, search);

        sql.append(" ORDER BY a.").append(safeSort).append(" ").append(safeDir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), ALERT_ROW_MAPPER, params.toArray());
    }

    public long countSearchAlerts(
            Alert.AlertStatus status, Alert.AlertSeverity severity,
            LocalDateTime fromDate, LocalDateTime toDate, String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM alerts a
                JOIN transactions t ON a.transaction_id = t.id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, status, severity, fromDate, toDate, search);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    public List<Alert> findByStatus(Alert.AlertStatus status, int offset, int limit) {
        String sql = """
                SELECT a.*, t.transaction_ref, t.account_id, t.customer_name,
                       r.rule_code, r.rule_name
                FROM alerts a
                JOIN transactions t ON a.transaction_id = t.id
                JOIN monitoring_rules r ON a.rule_id = r.id
                WHERE a.status = ?
                ORDER BY a.created_at DESC
                LIMIT ? OFFSET ?
                """;
        return jdbcTemplate.query(sql, ALERT_ROW_MAPPER, status.name(), limit, offset);
    }

    public long countByStatus(Alert.AlertStatus status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE status = ?", Long.class, status.name());
        return count != null ? count : 0L;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM alerts", Long.class);
        return count != null ? count : 0L;
    }

    public long countAlertsSince(LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE created_at >= ?",
                Long.class, Timestamp.valueOf(since));
        return count != null ? count : 0L;
    }

    /** Returns a list of {status, count} maps for charting. */
    public List<Map<String, Object>> countGroupByStatus() {
        String sql = "SELECT status, COUNT(*) AS cnt FROM alerts GROUP BY status";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("status", rs.getString("status"));
            row.put("count", rs.getLong("cnt"));
            return row;
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void appendFilters(StringBuilder sql, List<Object> params,
                               Alert.AlertStatus status, Alert.AlertSeverity severity,
                               LocalDateTime fromDate, LocalDateTime toDate, String search) {
        if (status != null) {
            sql.append(" AND a.status = ?");
            params.add(status.name());
        }
        if (severity != null) {
            sql.append(" AND a.severity = ?");
            params.add(severity.name());
        }
        if (fromDate != null) {
            sql.append(" AND a.created_at >= ?");
            params.add(Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND a.created_at <= ?");
            params.add(Timestamp.valueOf(toDate));
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (t.transaction_ref LIKE ? OR t.account_id LIKE ? OR t.customer_name LIKE ?)");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
    }

    private String allowedSortColumn(String sortBy) {
        return switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "status" -> "status";
            case "severity" -> "severity";
            case "createdat", "created_at" -> "created_at";
            case "updatedat", "updated_at" -> "updated_at";
            default -> "created_at";
        };
    }
}
