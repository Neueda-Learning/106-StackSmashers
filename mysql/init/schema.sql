-- Transaction Monitoring System (TMS) schema.
-- Auto-executed by the official MySQL image on first container startup
-- (only when the data volume is empty), against the database named by
-- MYSQL_DATABASE. Safe to re-run manually thanks to IF NOT EXISTS.
--
-- Column/type choices are derived directly from the JdbcTemplate SQL and
-- RowMapper code in backend/src/main/java/com/neueda/tms/repository/**.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ── users ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(255)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    role          VARCHAR(50)     NOT NULL DEFAULT 'ANALYST',
    is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── monitoring_rules ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS monitoring_rules (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rule_code   VARCHAR(100)    NOT NULL,
    rule_name   VARCHAR(255)    NOT NULL,
    description TEXT            NULL,
    severity    VARCHAR(20)     NOT NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    parameters  JSON            NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NULL,
    UNIQUE KEY uq_monitoring_rules_rule_code (rule_code),
    KEY idx_monitoring_rules_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── transactions ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transaction_ref  VARCHAR(255)   NOT NULL,
    account_id       VARCHAR(100)   NOT NULL,
    customer_name    VARCHAR(255)   NOT NULL,
    amount           DECIMAL(15,2)  NOT NULL,
    currency         VARCHAR(10)    NOT NULL,
    country_code     VARCHAR(10)    NOT NULL,
    transaction_type VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    is_new_customer  BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata         JSON           NULL,
    UNIQUE KEY uq_transactions_transaction_ref (transaction_ref),
    KEY idx_transactions_account_id (account_id),
    KEY idx_transactions_created_at (created_at),
    KEY idx_transactions_status (status),
    KEY idx_transactions_country_code (country_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── alerts ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alerts (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT UNSIGNED NOT NULL,
    rule_id        BIGINT UNSIGNED NOT NULL,
    status         VARCHAR(20)     NOT NULL DEFAULT 'OPEN',
    severity       VARCHAR(20)     NOT NULL,
    description    TEXT            NULL,
    assigned_to    VARCHAR(100)    NULL,
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME        NULL,
    KEY idx_alerts_status (status),
    KEY idx_alerts_severity (severity),
    KEY idx_alerts_created_at (created_at),
    KEY idx_alerts_transaction_id (transaction_id),
    CONSTRAINT fk_alerts_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions (id) ON DELETE CASCADE,
    CONSTRAINT fk_alerts_rule FOREIGN KEY (rule_id)
        REFERENCES monitoring_rules (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── alert_audit_trail ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alert_audit_trail (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    alert_id     BIGINT UNSIGNED NOT NULL,
    action       VARCHAR(30)     NOT NULL,
    performed_by VARCHAR(100)    NOT NULL,
    notes        TEXT            NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_alert_audit_trail_alert_id (alert_id),
    KEY idx_alert_audit_trail_created_at (created_at),
    CONSTRAINT fk_alert_audit_trail_alert FOREIGN KEY (alert_id)
        REFERENCES alerts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
