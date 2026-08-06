package com.neueda.tms.Dashboard;

import com.neueda.tms.controller.dashboard.DashboardController;
import com.neueda.tms.controller.dashboard.DashboardStatsDTO;
import com.neueda.tms.service.dashboard.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTests {

    @Mock
    private DashboardService dashboardService;

    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        dashboardController = new DashboardController(dashboardService);
    }

    @Test
    void dashboardStatsEndpointReturnsTheSnapshotPreparedByTheService() {
        DashboardStatsDTO stats = new DashboardStatsDTO(
                10L, 4L, 3L, 2L, 1L, 30.0,
                80L, 12L, 5L, 9L,
                List.of(Map.of("status", "OPEN", "count", 4L)),
                null
        );
        when(dashboardService.getDashboardStats()).thenReturn(stats);

        ResponseEntity<DashboardStatsDTO> response = dashboardController.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOpenAlerts()).isEqualTo(4L);
        assertThat(response.getBody().getForwardedAlerts()).isEqualTo(3L);
        assertThat(response.getBody().getTotalTransactions()).isEqualTo(80L);
        assertThat(response.getBody().getAlertsByStatus()).containsExactly(Map.of("status", "OPEN", "count", 4L));
    }

    @Test
    void dashboardStatsEndpointStillReturnsOkWhenThereIsNoActivityYet() {
        DashboardStatsDTO emptyStats = new DashboardStatsDTO(
                0L, 0L, 0L, 0L, 0L, 0.0,
                0L, 0L, 0L, 0L,
                List.of(),
                null
        );
        when(dashboardService.getDashboardStats()).thenReturn(emptyStats);

        ResponseEntity<DashboardStatsDTO> response = dashboardController.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalAlerts()).isZero();
        assertThat(response.getBody().getTotalTransactions()).isZero();
        assertThat(response.getBody().getAlertsByStatus()).isEmpty();
        assertThat(response.getBody().getAlertsByRule()).isNull();
    }
}

