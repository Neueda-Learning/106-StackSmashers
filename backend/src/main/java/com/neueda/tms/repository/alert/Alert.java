package com.neueda.tms.repository.alert;

import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;

import java.time.LocalDateTime;

public class Alert {

    private Long id;
    private Transaction transaction;
    private MonitoringRule rule;
    private AlertStatus status;
    private AlertSeverity severity;
    private String description;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Alert() {
        this.status = AlertStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public Alert(Long id,
            Transaction transaction,
            MonitoringRule rule,
            AlertStatus status,
            AlertSeverity severity,
            String description,
            String assignedTo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        this.id = id;
        this.transaction = transaction;
        this.rule = rule;
        this.status = (status != null) ? status : AlertStatus.OPEN;
        this.severity = severity;
        this.description = description;
        this.assignedTo = assignedTo;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public MonitoringRule getRule() {
        return rule;
    }

    public void setRule(MonitoringRule rule) {
        this.rule = rule;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "id=" + id +
                ", transaction=" + transaction +
                ", rule=" + rule +
                ", status=" + status +
                ", severity=" + severity +
                ", description='" + description + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Alert)) {
            return false;
        }

        Alert alert = (Alert) obj;

        return id != null && id.equals(alert.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public enum AlertStatus {
        OPEN,
        FORWARDED,
        DISMISSED,
        CLOSED
    }

    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}