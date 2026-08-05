package com.neueda.tms.service.dashboard;

import com.neueda.tms.service.alert.AlertService;

import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.dashboard.DashboardStatsDTO;
import com.neueda.tms.repository.alert.AlertRepository;
import com.neueda.tms.repository.transaction.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service layer for Dashboard statistics.
 * Centralises dashboard logic so the controller does not touch repositories directly.
 */
@Service
public class DashboardService implements IDashboardService {

    private final AlertService alertService;
    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public DashboardService(AlertService alertService,
                            AlertRepository alertRepository,
                            TransactionRepository transactionRepository) {
        this.alertService = alertService;
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        AlertDTO.StatsResponse alertStats = alertService.getStats();

        long totalTx = transactionRepository.countAll();
        long txLast24h = transactionRepository.countTransactionsSince(
                LocalDateTime.now().minusHours(24));

        List<Map<String, Object>> alertsByStatus = alertRepository.countGroupByStatus();

        return new DashboardStatsDTO(
                alertStats.getTotalAlerts(),
                alertStats.getOpenAlerts(),
                alertStats.getForwardedAlerts(),
                alertStats.getDismissedAlerts(),
                alertStats.getClosedAlerts(),
                alertStats.getPercentageForwarded(),
                totalTx,
                txLast24h,
                alertStats.getAlertsLast24h(),
                alertStats.getAlertsLast7d(),
                alertsByStatus,
                null // alertsByRule is not set in original code, passing null
        );
    }
}
