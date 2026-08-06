package com.neueda.tms.Dashboard;

import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.dashboard.DashboardStatsDTO;
import com.neueda.tms.repository.alert.AlertRepository;
import com.neueda.tms.repository.transaction.TransactionRepository;
import com.neueda.tms.service.alert.AlertService;
import com.neueda.tms.service.dashboard.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTests {

    @Mock
    private AlertService alertService;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(alertService, alertRepository, transactionRepository);
    }

    @Test
    void dashboardCombinesAlertStatsAndTransactionCountsIntoOneSnapshot() {
        when(alertService.getStats()).thenReturn(new AlertDTO.StatsResponse(50L, 20L, 15L, 10L, 5L, 30.0, 12L, 25L));
        when(transactionRepository.countAll()).thenReturn(200L);
        when(transactionRepository.countTransactionsSince(any())).thenReturn(40L);
        when(alertRepository.countGroupByStatus()).thenReturn(
                List.of(Map.of("status", "OPEN", "count", 20L), Map.of("status", "FORWARDED", "count", 15L)));

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertThat(stats.getTotalAlerts()).isEqualTo(50L);
        assertThat(stats.getOpenAlerts()).isEqualTo(20L);
        assertThat(stats.getForwardedAlerts()).isEqualTo(15L);
        assertThat(stats.getDismissedAlerts()).isEqualTo(10L);
        assertThat(stats.getClosedAlerts()).isEqualTo(5L);
        assertThat(stats.getPercentageForwarded()).isEqualTo(30.0);
        assertThat(stats.getTotalTransactions()).isEqualTo(200L);
        assertThat(stats.getTransactionsLast24h()).isEqualTo(40L);
        assertThat(stats.getAlertsLast24h()).isEqualTo(12L);
        assertThat(stats.getAlertsLast7d()).isEqualTo(25L);
        assertThat(stats.getAlertsByStatus()).hasSize(2);
        assertThat(stats.getAlertsByRule()).isNull();
    }

    @Test
    void dashboardReturnsAllZeroesWhenThereIsNothingInTheSystem() {
        when(alertService.getStats()).thenReturn(new AlertDTO.StatsResponse(0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L));
        when(transactionRepository.countAll()).thenReturn(0L);
        when(transactionRepository.countTransactionsSince(any())).thenReturn(0L);
        when(alertRepository.countGroupByStatus()).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertThat(stats.getTotalAlerts()).isZero();
        assertThat(stats.getTotalTransactions()).isZero();
        assertThat(stats.getTransactionsLast24h()).isZero();
        assertThat(stats.getAlertsLast24h()).isZero();
        assertThat(stats.getAlertsLast7d()).isZero();
        assertThat(stats.getPercentageForwarded()).isZero();
        assertThat(stats.getAlertsByStatus()).isEmpty();
    }

    @Test
    void dashboardPassesTheLast24hTransactionCountThroughCorrectly() {
        when(alertService.getStats()).thenReturn(new AlertDTO.StatsResponse(5L, 3L, 1L, 1L, 0L, 20.0, 2L, 4L));
        when(transactionRepository.countAll()).thenReturn(120L);
        when(transactionRepository.countTransactionsSince(any())).thenReturn(18L);
        when(alertRepository.countGroupByStatus()).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertThat(stats.getTransactionsLast24h()).isEqualTo(18L);
        assertThat(stats.getTotalTransactions()).isEqualTo(120L);
    }

    @Test
    void chartDataPreservesTheGroupingReturnedByTheRepository() {
        when(alertService.getStats()).thenReturn(new AlertDTO.StatsResponse(8L, 3L, 2L, 2L, 1L, 25.0, 3L, 7L));
        when(transactionRepository.countAll()).thenReturn(30L);
        when(transactionRepository.countTransactionsSince(any())).thenReturn(5L);
        List<Map<String, Object>> chartData = List.of(
                Map.of("status", "OPEN", "count", 3L),
                Map.of("status", "DISMISSED", "count", 2L),
                Map.of("status", "CLOSED", "count", 1L)
        );
        when(alertRepository.countGroupByStatus()).thenReturn(chartData);

        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertThat(stats.getAlertsByStatus()).containsExactlyElementsOf(chartData);
    }
}
