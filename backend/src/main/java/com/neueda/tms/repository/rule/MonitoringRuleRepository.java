package com.neueda.tms.repository.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-based repository for MonitoringRule entities.
 * All queries use parameterized statements to prevent SQL injection.
 */
@Repository
public class MonitoringRuleRepository {
    private static final Logger log = LoggerFactory.getLogger(MonitoringRuleRepository.class);


    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MonitoringRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── RowMapper ──────────────────────────────────────────────────────────────

    private static final RowMapper<MonitoringRule> ROW_MAPPER = (rs, rowNum) -> {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(rs.getLong("id"));
        rule.setRuleCode(rs.getString("rule_code"));
        rule.setRuleName(rs.getString("rule_name"));
        rule.setDescription(rs.getString("description"));
        String severity = rs.getString("severity");
        if (severity != null) {
            rule.setSeverity(MonitoringRule.RuleSeverity.valueOf(severity));
        }
        rule.setIsActive(rs.getBoolean("is_active"));
        // parameters JSON column deserialized by Jackson via a utility method
        rule.setParameters(parseJson(rs.getString("parameters")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        rule.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        rule.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        return rule;
    };

    // ── Write Operations ───────────────────────────────────────────────────────

    public MonitoringRule save(MonitoringRule rule) {
        if (rule.getId() == null) {
            return insert(rule);
        } else {
            update(rule);
            return rule;
        }
    }

    private MonitoringRule insert(MonitoringRule rule) {
        String sql = """
                INSERT INTO monitoring_rules
                  (rule_code, rule_name, description, severity, is_active, parameters, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, rule.getRuleCode());
            ps.setString(2, rule.getRuleName());
            ps.setString(3, rule.getDescription());
            ps.setString(4, rule.getSeverity() != null ? rule.getSeverity().name() : null);
            ps.setBoolean(5, rule.getIsActive() != null && rule.getIsActive());
            ps.setString(6, toJson(rule.getParameters()));
            ps.setTimestamp(7, Timestamp.valueOf(
                    rule.getCreatedAt() != null ? rule.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(8, rule.getUpdatedAt() != null ? Timestamp.valueOf(rule.getUpdatedAt()) : null);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) rule.setId(key.longValue());
        return rule;
    }

    private void update(MonitoringRule rule) {
        String sql = """
                UPDATE monitoring_rules
                SET rule_name = ?, description = ?, severity = ?,
                    is_active = ?, parameters = ?, updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                rule.getRuleName(),
                rule.getDescription(),
                rule.getSeverity() != null ? rule.getSeverity().name() : null,
                rule.getIsActive() != null && rule.getIsActive(),
                toJson(rule.getParameters()),
                rule.getUpdatedAt() != null ? Timestamp.valueOf(rule.getUpdatedAt()) : null,
                rule.getId());
    }

    // ── Read Operations ────────────────────────────────────────────────────────

    public List<MonitoringRule> findAll() {
        return jdbcTemplate.query("SELECT * FROM monitoring_rules ORDER BY id", ROW_MAPPER);
    }

    public Optional<MonitoringRule> findById(Long id) {
        try {
            MonitoringRule rule = jdbcTemplate.queryForObject(
                    "SELECT * FROM monitoring_rules WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(rule);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<MonitoringRule> findByRuleCode(String ruleCode) {
        try {
            MonitoringRule rule = jdbcTemplate.queryForObject(
                    "SELECT * FROM monitoring_rules WHERE rule_code = ?", ROW_MAPPER, ruleCode);
            return Optional.ofNullable(rule);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<MonitoringRule> findByIsActiveTrue() {
        return jdbcTemplate.query(
                "SELECT * FROM monitoring_rules WHERE is_active = TRUE ORDER BY id", ROW_MAPPER);
    }

    public boolean existsByRuleCode(String ruleCode) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM monitoring_rules WHERE rule_code = ?", Long.class, ruleCode);
        return count != null && count > 0;
    }

    // ── JSON helpers ───────────────────────────────────────────────────────────

    /**
     * Serialize parameters map to JSON string for storage.
     * Uses Jackson ObjectMapper for safe serialization.
     */
    private static String toJson(Map<String, Object> params) {
        if (params == null || params.isEmpty()) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to serialize rule parameters to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deserialize parameters JSON string from DB back to map.
     */
    private static Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(
                    json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse rule parameters JSON: {}", e.getMessage());
            return null;
        }
    }
}
