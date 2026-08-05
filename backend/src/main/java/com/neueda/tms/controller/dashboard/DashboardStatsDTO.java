package com.neueda.tms.controller.dashboard;

import java.util.List;
import java.util.Map;

public class DashboardStatsDTO {
    // Alerts
    private long totalAlerts;
    private long openAlerts;
    private long forwardedAlerts;
    private long dismissedAlerts;
    private long closedAlerts;
    private double percentageForwarded;

    // Transactions
    private long totalTransactions;
    private long transactionsLast24h;

    // Recent activity
    private long alertsLast24h;
    private long alertsLast7d;

    // Chart data
    private List<Map<String, Object>> alertsByStatus;
    private List<Map<String, Object>> alertsByRule;

    public DashboardStatsDTO() {}

    public DashboardStatsDTO(long totalAlerts, long openAlerts, long forwardedAlerts, long dismissedAlerts, long closedAlerts, double percentageForwarded, long totalTransactions, long transactionsLast24h, long alertsLast24h, long alertsLast7d, List<Map<String, Object>> alertsByStatus, List<Map<String, Object>> alertsByRule) {
        this.totalAlerts = totalAlerts;
        this.openAlerts = openAlerts;
        this.forwardedAlerts = forwardedAlerts;
        this.dismissedAlerts = dismissedAlerts;
        this.closedAlerts = closedAlerts;
        this.percentageForwarded = percentageForwarded;
        this.totalTransactions = totalTransactions;
        this.transactionsLast24h = transactionsLast24h;
        this.alertsLast24h = alertsLast24h;
        this.alertsLast7d = alertsLast7d;
        this.alertsByStatus = alertsByStatus;
        this.alertsByRule = alertsByRule;
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
    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }
    public long getTransactionsLast24h() { return transactionsLast24h; }
    public void setTransactionsLast24h(long transactionsLast24h) { this.transactionsLast24h = transactionsLast24h; }
    public long getAlertsLast24h() { return alertsLast24h; }
    public void setAlertsLast24h(long alertsLast24h) { this.alertsLast24h = alertsLast24h; }
    public long getAlertsLast7d() { return alertsLast7d; }
    public void setAlertsLast7d(long alertsLast7d) { this.alertsLast7d = alertsLast7d; }
    public List<Map<String, Object>> getAlertsByStatus() { return alertsByStatus; }
    public void setAlertsByStatus(List<Map<String, Object>> alertsByStatus) { this.alertsByStatus = alertsByStatus; }
    public List<Map<String, Object>> getAlertsByRule() { return alertsByRule; }
    public void setAlertsByRule(List<Map<String, Object>> alertsByRule) { this.alertsByRule = alertsByRule; }
}
