package com.neueda.tms.Alerts;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertsRepositoryTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        alertRepository = new AlertRepository(jdbcTemplate);
    }

    @Test
    void lookingUpAnAlertByIdGivesBackTheCorrectAlert() {
        Alert expected = new Alert();
        expected.setId(1L);
        expected.setStatus(Alert.AlertStatus.OPEN);
        expected.setSeverity(Alert.AlertSeverity.HIGH);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(expected);

        Optional<Alert> found = alertRepository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
        assertThat(found.get().getStatus()).isEqualTo(Alert.AlertStatus.OPEN);
        assertThat(found.get().getSeverity()).isEqualTo(Alert.AlertSeverity.HIGH);
    }

    @Test
    void lookingUpAnAlertThatDoesNotExistReturnsNothing() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(999L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<Alert> found = alertRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void aTransactionWithMultipleFlagsReturnsAllOfItsAlerts() {
        Alert first = new Alert();
        first.setId(10L);
        Alert second = new Alert();
        second.setId(11L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(42L)))
                .thenReturn(List.of(first, second));

        List<Alert> alerts = alertRepository.findByTransactionId(42L);

        assertThat(alerts).hasSize(2);
        assertThat(alerts).extracting(Alert::getId).containsExactly(10L, 11L);
    }

    @Test
    void aCleanTransactionWithNoRuleBreachesComesBbackWithAnEmptyAlertList() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(99L)))
                .thenReturn(List.of());

        List<Alert> alerts = alertRepository.findByTransactionId(99L);

        assertThat(alerts).isEmpty();
    }

    @Test
    void searchingByAccountIdBringsUpAllFlaggedActivityForThatAccount() {
        Alert flag = new Alert();
        flag.setId(5L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("ACCT-123")))
                .thenReturn(List.of(flag));

        List<Alert> alerts = alertRepository.findByAccountId("ACCT-123");

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getId()).isEqualTo(5L);
    }

    @Test
    void theTotalAlertCountMatchesWhatIsStoredInTheDatabase() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(42L);

        assertThat(alertRepository.count()).isEqualTo(42L);
    }

    @Test
    void aBrandNewSystemWithNoAlertsYetReportsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(0L);

        assertThat(alertRepository.count()).isZero();
    }

    @Test
    void aNullResponseFromTheDatabaseIsSafelyTreatedAsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);

        assertThat(alertRepository.count()).isZero();
    }

    @Test
    void countingOpenAlertsReflectsHowManyInvestigationsAreStillPending() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("OPEN")))
                .thenReturn(7L);

        assertThat(alertRepository.countByStatus(Alert.AlertStatus.OPEN)).isEqualTo(7L);
    }

    @Test
    void alertsRaisedInTheLast24HoursAreCountedCorrectly() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any()))
                .thenReturn(15L);

        assertThat(alertRepository.countAlertsSince(since)).isEqualTo(15L);
    }

    @Test
    void theDashboardChartGetsOneRowPerAlertStatusFromTheDatabase() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(
                        Map.of("status", "OPEN", "count", 10L),
                        Map.of("status", "CLOSED", "count", 5L)
                ));

        List<Map<String, Object>> chart = alertRepository.countGroupByStatus();

        assertThat(chart).hasSize(2);
        assertThat(chart.get(0)).containsEntry("status", "OPEN").containsEntry("count", 10L);
        assertThat(chart.get(1)).containsEntry("status", "CLOSED").containsEntry("count", 5L);
    }

    @Test
    void theDashboardChartIsEmptyWhenNoAlertsHaveBeenRaisedYet() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of());

        assertThat(alertRepository.countGroupByStatus()).isEmpty();
    }

    @Test
    void analystCanPageThroughOpenAlertsAndSeeOnlyTheRightOnes() {
        Alert openAlert = new Alert();
        openAlert.setId(1L);
        openAlert.setStatus(Alert.AlertStatus.OPEN);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("OPEN"), eq(10), eq(0)))
                .thenReturn(List.of(openAlert));

        List<Alert> page = alertRepository.findByStatus(Alert.AlertStatus.OPEN, 0, 10);

        assertThat(page).hasSize(1);
        assertThat(page.get(0).getStatus()).isEqualTo(Alert.AlertStatus.OPEN);
    }

    @Test
    void pagingThroughDismissedAlertsWhenThereAreNoneReturnsAnEmptyPage() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("DISMISSED"), eq(10), eq(0)))
                .thenReturn(List.of());

        List<Alert> page = alertRepository.findByStatus(Alert.AlertStatus.DISMISSED, 0, 10);

        assertThat(page).isEmpty();
    }
}
