package com.neueda.tms.repository.alert;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertAuditTrail;
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
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based repository for AlertAuditTrail entities.
 * All queries use parameterized statements to prevent SQL injection.
 */
@Repository
public class AlertAuditTrailRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AlertAuditTrailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── RowMapper ──────────────────────────────────────────────────────────────

    /**
     * Joins alert_audit_trail → alerts → transactions to load all required fields
     * in one query.
     */
    private static final RowMapper<AlertAuditTrail> ROW_MAPPER = (rs, rowNum) -> {
        AlertAuditTrail entry = new AlertAuditTrail();
        entry.setId(rs.getLong("id"));
        entry.setAction(AlertAuditTrail.AuditAction.valueOf(rs.getString("action")));
        entry.setPerformedBy(rs.getString("performed_by"));
        entry.setNotes(rs.getString("notes"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        entry.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        Alert alert = new Alert();
        alert.setId(rs.getLong("alert_id"));
        Transaction tx = new Transaction();
        tx.setId(rs.getLong("transaction_id"));
        tx.setTransactionRef(rs.getString("transaction_ref"));
        tx.setAccountId(rs.getString("account_id"));
        alert.setTransaction(tx);
        entry.setAlert(alert);

        return entry;
    };

    // ── Write Operations ───────────────────────────────────────────────────────

    public AlertAuditTrail save(AlertAuditTrail entry) {
        String sql = """
                INSERT INTO alert_audit_trail (alert_id, action, performed_by, notes, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, entry.getAlert().getId());
            ps.setString(2, entry.getAction().name());
            ps.setString(3, entry.getPerformedBy());
            ps.setString(4, entry.getNotes());
            ps.setTimestamp(5, Timestamp.valueOf(
                    entry.getCreatedAt() != null ? entry.getCreatedAt() : LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null)
            entry.setId(key.longValue());
        return entry;
    }

    // ── Read Operations ────────────────────────────────────────────────────────

    public List<AlertAuditTrail> findByAlertIdOrderByCreatedAtAsc(Long alertId) {
        String sql = """
                SELECT aat.id, aat.alert_id, aat.action, aat.performed_by, aat.notes, aat.created_at,
                       a.transaction_id, t.transaction_ref, t.account_id
                FROM alert_audit_trail aat
                JOIN alerts a ON aat.alert_id = a.id
                JOIN transactions t ON a.transaction_id = t.id
                WHERE aat.alert_id = ?
                ORDER BY aat.created_at ASC
                """;
        return jdbcTemplate.query(sql, ROW_MAPPER, alertId);
    }

    /**
     * Paginated audit report with optional date and action filters.
     */
    public List<AlertAuditTrail> findAuditReport(
            LocalDateTime fromDate, LocalDateTime toDate,
            AlertAuditTrail.AuditAction action,
            int offset, int limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT aat.id, aat.alert_id, aat.action, aat.performed_by, aat.notes, aat.created_at,
                       a.transaction_id, t.transaction_ref, t.account_id
                FROM alert_audit_trail aat
                JOIN alerts a ON aat.alert_id = a.id
                JOIN transactions t ON a.transaction_id = t.id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append(" AND aat.created_at >= ?");
            params.add(Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND aat.created_at <= ?");
            params.add(Timestamp.valueOf(toDate));
        }
        if (action != null) {
            sql.append(" AND aat.action = ?");
            params.add(action.name());
        }

        sql.append(" ORDER BY aat.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public long countAuditReport(LocalDateTime fromDate, LocalDateTime toDate,
            AlertAuditTrail.AuditAction action) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM alert_audit_trail aat WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append(" AND aat.created_at >= ?");
            params.add(Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND aat.created_at <= ?");
            params.add(Timestamp.valueOf(toDate));
        }
        if (action != null) {
            sql.append(" AND aat.action = ?");
            params.add(action.name());
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }
}
