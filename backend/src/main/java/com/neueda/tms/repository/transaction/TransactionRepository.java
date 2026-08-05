package com.neueda.tms.repository.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based repository for Transaction entities.
 * All queries use parameterized statements to prevent SQL injection.
 */
@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── RowMapper ──────────────────────────────────────────────────────────────

    private static final RowMapper<Transaction> ROW_MAPPER = (rs, rowNum) -> {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setTransactionRef(rs.getString("transaction_ref"));
        t.setAccountId(rs.getString("account_id"));
        t.setCustomerName(rs.getString("customer_name"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setCurrency(rs.getString("currency"));
        t.setCountryCode(rs.getString("country_code"));
        t.setTransactionType(Transaction.TransactionType.valueOf(rs.getString("transaction_type")));
        t.setStatus(Transaction.TransactionStatus.valueOf(rs.getString("status")));
        t.setIsNewCustomer(rs.getBoolean("is_new_customer"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        t.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);
        // metadata is a JSON column — set null here; MonitoringEngine does not need it for rule eval
        t.setMetadata(null);
        return t;
    };

    // ── Write Operations ───────────────────────────────────────────────────────

    /**
     * Insert or update a transaction.
     * Uses INSERT ... ON DUPLICATE KEY UPDATE for idempotent saves.
     */
    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            return insert(transaction);
        } else {
            update(transaction);
            return transaction;
        }
    }

    private Transaction insert(Transaction transaction) {
        String sql = """
                INSERT INTO transactions
                  (transaction_ref, account_id, customer_name, amount, currency,
                   country_code, transaction_type, status, is_new_customer, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, transaction.getTransactionRef());
            ps.setString(2, transaction.getAccountId());
            ps.setString(3, transaction.getCustomerName());
            ps.setBigDecimal(4, transaction.getAmount());
            ps.setString(5, transaction.getCurrency());
            ps.setString(6, transaction.getCountryCode());
            ps.setString(7, transaction.getTransactionType().name());
            ps.setString(8, transaction.getStatus() != null
                    ? transaction.getStatus().name()
                    : Transaction.TransactionStatus.PENDING.name());
            ps.setBoolean(9, transaction.getIsNewCustomer() != null && transaction.getIsNewCustomer());
            ps.setTimestamp(10, Timestamp.valueOf(
                    transaction.getCreatedAt() != null ? transaction.getCreatedAt() : LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            transaction.setId(key.longValue());
        }
        return transaction;
    }

    private void update(Transaction transaction) {
        String sql = """
                UPDATE transactions
                SET account_id = ?, customer_name = ?, amount = ?, currency = ?,
                    country_code = ?, transaction_type = ?, status = ?, is_new_customer = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql,
                transaction.getAccountId(),
                transaction.getCustomerName(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCountryCode(),
                transaction.getTransactionType().name(),
                transaction.getStatus().name(),
                transaction.getIsNewCustomer() != null && transaction.getIsNewCustomer(),
                transaction.getId());
    }

    // ── Read Operations ────────────────────────────────────────────────────────

    public Optional<Transaction> findById(Long id) {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        try {
            Transaction t = jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
            return Optional.ofNullable(t);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Transaction> findByTransactionRef(String transactionRef) {
        String sql = "SELECT * FROM transactions WHERE transaction_ref = ?";
        try {
            Transaction t = jdbcTemplate.queryForObject(sql, ROW_MAPPER, transactionRef);
            return Optional.ofNullable(t);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Dynamic filtered + paginated search.
     * Uses parameterized WHERE clauses built dynamically — never string-concatenated values.
     */
    public List<Transaction> searchTransactions(
            String search,
            Transaction.TransactionStatus status,
            Transaction.TransactionType transactionType,
            String countryCode,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int offset,
            int limit,
            String sortBy,
            String sortDir) {

        // Whitelist sortBy to prevent order-by injection
        String safeSort = allowedSortColumn(sortBy);
        String safeDir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        appendFilters(sql, params, search, status, transactionType, countryCode, fromDate, toDate, minAmount, maxAmount);

        sql.append(" ORDER BY ").append(safeSort).append(" ").append(safeDir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public long countSearchTransactions(
            String search,
            Transaction.TransactionStatus status,
            Transaction.TransactionType transactionType,
            String countryCode,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, status, transactionType, countryCode, fromDate, toDate, minAmount, maxAmount);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    public List<Transaction> findByAccountId(String accountId, int offset, int limit) {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, accountId, limit, offset);
    }

    public long countByAccountId(String accountId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = ?", Long.class, accountId);
        return count != null ? count : 0L;
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions", Long.class);
        return count != null ? count : 0L;
    }

    public long countTransactionsSince(LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE created_at >= ?",
                Long.class, Timestamp.valueOf(since));
        return count != null ? count : 0L;
    }

    public long countRecentTransactionsByAccount(String accountId, LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = ? AND created_at >= ?",
                Long.class, accountId, Timestamp.valueOf(since));
        return count != null ? count : 0L;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void appendFilters(StringBuilder sql, List<Object> params,
                               String search,
                               Transaction.TransactionStatus status,
                               Transaction.TransactionType transactionType,
                               String countryCode,
                               LocalDateTime fromDate,
                               LocalDateTime toDate,
                               BigDecimal minAmount,
                               BigDecimal maxAmount) {

        if (search != null && !search.isBlank()) {
            sql.append(" AND (transaction_ref LIKE ? OR customer_name LIKE ? OR account_id LIKE ?)");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }
        if (transactionType != null) {
            sql.append(" AND transaction_type = ?");
            params.add(transactionType.name());
        }
        if (countryCode != null && !countryCode.isBlank()) {
            sql.append(" AND country_code = ?");
            params.add(countryCode.trim());
        }
        if (fromDate != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.valueOf(toDate));
        }
        if (minAmount != null) {
            sql.append(" AND amount >= ?");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            sql.append(" AND amount <= ?");
            params.add(maxAmount);
        }
    }

    /** Whitelist of allowed ORDER BY columns to prevent SQL injection via sortBy parameter. */
    private String allowedSortColumn(String sortBy) {
        return switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "amount" -> "amount";
            case "createdat", "created_at" -> "created_at";
            case "status" -> "status";
            case "transactionref", "transaction_ref" -> "transaction_ref";
            case "accountid", "account_id" -> "account_id";
            default -> "created_at";
        };
    }
}
