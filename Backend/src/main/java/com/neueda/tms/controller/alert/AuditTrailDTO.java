package com.neueda.tms.controller.alert;

import com.neueda.tms.repository.alert.AlertAuditTrail;
import java.time.LocalDateTime;

public class AuditTrailDTO {
    private Long id;
    private Long alertId;
    private Long transactionId;
    private String transactionRef;
    private String accountId;
    private AlertAuditTrail.AuditAction action;
    private String performedBy;
    private String notes;
    private LocalDateTime createdAt;

    public AuditTrailDTO() {}

    public AuditTrailDTO(Long id, Long alertId, Long transactionId, String transactionRef, String accountId, AlertAuditTrail.AuditAction action, String performedBy, String notes, LocalDateTime createdAt) {
        this.id = id;
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.transactionRef = transactionRef;
        this.accountId = accountId;
        this.action = action;
        this.performedBy = performedBy;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public AlertAuditTrail.AuditAction getAction() { return action; }
    public void setAction(AlertAuditTrail.AuditAction action) { this.action = action; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
