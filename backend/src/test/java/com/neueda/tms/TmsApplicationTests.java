package com.neueda.tms;

import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.dashboard.DashboardStatsDTO;
import com.neueda.tms.repository.alert.AlertRepository;
import com.neueda.tms.repository.transaction.TransactionRepository;
import com.neueda.tms.service.alert.AlertService;
import com.neueda.tms.service.dashboard.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmsApplicationTests {

    @Mock
    private AlertService alertService;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void returnDashboardStats_whenAllSourcesProvideData() {
        AlertDTO.StatsResponse stats = new AlertDTO.StatsResponse(
                50L, 20L, 15L, 10L, 5L, 30.0, 12L, 25L);
        List<Map<String, Object>> alertsByStatus = List.of(
                Map.of("status", "OPEN", "count", 20L),
                Map.of("status", "FORWARDED", "count", 15L));

        when(alertService.getStats()).thenReturn(stats);
        when(transactionRepository.countAll()).thenReturn(200L);
        when(transactionRepository.countTransactionsSince(ArgumentMatchers.any())).thenReturn(40L);
        when(alertRepository.countGroupByStatus()).thenReturn(alertsByStatus);

        DashboardStatsDTO result = dashboardService.getDashboardStats();

        assertThat(result.getTotalAlerts()).isEqualTo(50L);
        assertThat(result.getOpenAlerts()).isEqualTo(20L);
        assertThat(result.getForwardedAlerts()).isEqualTo(15L);
        assertThat(result.getDismissedAlerts()).isEqualTo(10L);
        assertThat(result.getClosedAlerts()).isEqualTo(5L);
        assertThat(result.getPercentageForwarded()).isEqualTo(30.0);
        assertThat(result.getTotalTransactions()).isEqualTo(200L);
        assertThat(result.getTransactionsLast24h()).isEqualTo(40L);
        assertThat(result.getAlertsLast24h()).isEqualTo(12L);
        assertThat(result.getAlertsLast7d()).isEqualTo(25L);
        assertThat(result.getAlertsByStatus()).isEqualTo(alertsByStatus);
        assertThat(result.getAlertsByRule()).isNull();
    }

    @Test
    void returnZeroBasedStats_whenThereIsNoActivity() {
        AlertDTO.StatsResponse stats = new AlertDTO.StatsResponse(
                0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L);

        when(alertService.getStats()).thenReturn(stats);
        when(transactionRepository.countAll()).thenReturn(0L);
        when(transactionRepository.countTransactionsSince(ArgumentMatchers.any())).thenReturn(0L);
        when(alertRepository.countGroupByStatus()).thenReturn(List.of());

        DashboardStatsDTO result = dashboardService.getDashboardStats();

        assertThat(result.getTotalAlerts()).isZero();
        assertThat(result.getTotalTransactions()).isZero();
        assertThat(result.getTransactionsLast24h()).isZero();
        assertThat(result.getAlertsLast24h()).isZero();
        assertThat(result.getAlertsLast7d()).isZero();
        assertThat(result.getAlertsByStatus()).isEmpty();
    }

    @Test
    void reflectRecentAlertSpike_whenLast24hCountIsHigherThanLast7dAverage() {
        AlertDTO.StatsResponse stats = new AlertDTO.StatsResponse(
                100L, 40L, 30L, 20L, 10L, 30.0, 50L, 70L);

        when(alertService.getStats()).thenReturn(stats);
        when(transactionRepository.countAll()).thenReturn(500L);
        when(transactionRepository.countTransactionsSince(ArgumentMatchers.any())).thenReturn(80L);
        when(alertRepository.countGroupByStatus()).thenReturn(List.of());

        DashboardStatsDTO result = dashboardService.getDashboardStats();

        assertThat(result.getAlertsLast24h()).isEqualTo(50L);
        assertThat(result.getAlertsLast7d()).isEqualTo(70L);
        assertThat(result.getAlertsLast24h()).isLessThan(result.getAlertsLast7d());
    }

    @Test
    void preserveChartGrouping_whenRepositoryReturnsMultipleStatuses() {
        AlertDTO.StatsResponse stats = new AlertDTO.StatsResponse(
                8L, 3L, 2L, 1L, 2L, 25.0, 3L, 7L);
        List<Map<String, Object>> alertsByStatus = List.of(
                Map.of("status", "OPEN", "count", 3L),
                Map.of("status", "DISMISSED", "count", 1L),
                Map.of("status", "CLOSED", "count", 2L));

        when(alertService.getStats()).thenReturn(stats);
        when(transactionRepository.countAll()).thenReturn(14L);
        when(transactionRepository.countTransactionsSince(ArgumentMatchers.any())).thenReturn(5L);
        when(alertRepository.countGroupByStatus()).thenReturn(alertsByStatus);

        DashboardStatsDTO result = dashboardService.getDashboardStats();

        assertThat(result.getAlertsByStatus()).hasSize(3);
        assertThat(result.getAlertsByStatus()).containsExactlyElementsOf(alertsByStatus);
    }

    @Test
    void calculatePercentageForwarded_asProvidedByAlertService() {
        AlertDTO.StatsResponse stats = new AlertDTO.StatsResponse(
                200L, 80L, 60L, 40L, 20L, 30.0, 10L, 50L);

        when(alertService.getStats()).thenReturn(stats);
        when(transactionRepository.countAll()).thenReturn(1000L);
        when(transactionRepository.countTransactionsSince(ArgumentMatchers.any())).thenReturn(100L);
        when(alertRepository.countGroupByStatus()).thenReturn(List.of());

        DashboardStatsDTO result = dashboardService.getDashboardStats();

        assertThat(result.getPercentageForwarded()).isEqualTo(30.0);
        assertThat(result.getForwardedAlerts()).isEqualTo(60L);
        assertThat(result.getTotalAlerts()).isEqualTo(200L);
    }
}

