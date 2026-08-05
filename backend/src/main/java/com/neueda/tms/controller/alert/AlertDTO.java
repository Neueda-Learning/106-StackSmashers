package com.neueda.tms.controller.alert;

import com.neueda.tms.repository.alert.Alert;

import java.time.LocalDateTime;

public class AlertDTO {

    public static class Response {
        private Long id;
        private Long transactionId;
        private String transactionRef;
        private String accountId;
        private String customerName;
        private Long ruleId;
        private String ruleCode;
        private String ruleName;
        private Alert.AlertStatus status;
        private Alert.AlertSeverity severity;
        private String description;
        private String assignedTo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Response() {}

        public Response(Long id, Long transactionId, String transactionRef, String accountId, String customerName, Long ruleId, String ruleCode, String ruleName, Alert.AlertStatus status, Alert.AlertSeverity severity, String description, String assignedTo, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.transactionId = transactionId;
            this.transactionRef = transactionRef;
            this.accountId = accountId;
            this.customerName = customerName;
            this.ruleId = ruleId;
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
            this.status = status;
            this.severity = severity;
            this.description = description;
            this.assignedTo = assignedTo;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public Long getRuleId() { return ruleId; }
        public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
        public String getRuleCode() { return ruleCode; }
        public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public Alert.AlertStatus getStatus() { return status; }
        public void setStatus(Alert.AlertStatus status) { this.status = status; }
        public Alert.AlertSeverity getSeverity() { return severity; }
        public void setSeverity(Alert.AlertSeverity severity) { this.severity = severity; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class ActionRequest {
        private String notes;
        private String assignedTo;

        public ActionRequest() {}

        public ActionRequest(String notes, String assignedTo) {
            this.notes = notes;
            this.assignedTo = assignedTo;
        }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    }

    public static class StatsResponse {
        private long totalAlerts;
        private long openAlerts;
        private long forwardedAlerts;
        private long dismissedAlerts;
        private long closedAlerts;
        private double percentageForwarded;
        private long alertsLast24h;
        private long alertsLast7d;

        public StatsResponse() {}

        public StatsResponse(long totalAlerts, long openAlerts, long forwardedAlerts, long dismissedAlerts, long closedAlerts, double percentageForwarded, long alertsLast24h, long alertsLast7d) {
            this.totalAlerts = totalAlerts;
            this.openAlerts = openAlerts;
            this.forwardedAlerts = forwardedAlerts;
            this.dismissedAlerts = dismissedAlerts;
            this.closedAlerts = closedAlerts;
            this.percentageForwarded = percentageForwarded;
            this.alertsLast24h = alertsLast24h;
            this.alertsLast7d = alertsLast7d;
        }

        public long getTotalAlerts() { return totalAlerts; }
        public void setTotalAlerts(long totalAlerts) { this.totalAlerts = totalAlerts; }
        public long getOpenAlerts() { return openAlerts; }
        public void setOpenAlerts(long openAlerts) { this.openAlerts = openAlerts; }
        public long getForwardedAlerts() { return forwardedAlerts; }
        public void setForwardedAlerts(long forwardedAlerts) { this.forwardedAlerts = forwardedAlerts; }
        public long getDismissedAlerts() { return dismissedAlerts; }
        public void setDismissedAlerts(long dismissedAlerts) { this.dismissedAlerts = dismissedAlerts; }
        public long getClosedAlerts() { return closedAlerts; }
        public void setClosedAlerts(long closedAlerts) { this.closedAlerts = closedAlerts; }
        public double getPercentageForwarded() { return percentageForwarded; }
        public void setPercentageForwarded(double percentageForwarded) { this.percentageForwarded = percentageForwarded; }
        public long getAlertsLast24h() { return alertsLast24h; }
        public void setAlertsLast24h(long alertsLast24h) { this.alertsLast24h = alertsLast24h; }
        public long getAlertsLast7d() { return alertsLast7d; }
        public void setAlertsLast7d(long alertsLast7d) { this.alertsLast7d = alertsLast7d; }
    }
}
