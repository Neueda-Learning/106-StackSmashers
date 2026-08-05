package com.neueda.tms.repository.alert;

import java.time.LocalDateTime;

public class AlertAuditTrail {

    private Long id;
    private Alert alert;
    private AuditAction action;
    private String performedBy;
    private String notes;
    private LocalDateTime createdAt = LocalDateTime.now();

    public AlertAuditTrail() {
    }

    public AlertAuditTrail(Alert alert, AuditAction action, String performedBy, String notes) {
        this.alert = alert;
        this.action = action;
        this.performedBy = performedBy;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public enum AuditAction {
        CREATED, FORWARDED, DISMISSED, CLOSED, NOTE_ADDED, REOPENED
    }
}
