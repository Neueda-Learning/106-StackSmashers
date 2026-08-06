package com.neueda.tms.Alerts;

import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertAuditTrail;
import com.neueda.tms.repository.alert.AlertAuditTrailRepository;
import com.neueda.tms.repository.alert.AlertRepository;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.service.alert.AlertService;
import com.neueda.tms.service.alert.AuditTrailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTests {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertAuditTrailRepository auditTrailRepository;

    @Mock
    private AuditTrailService auditTrailService;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, auditTrailRepository, auditTrailService);
    }

    @Test
    void analystCanLookUpAnAlertAndGetAllItsDetails() {
        Alert alert = anOpenAlert(1L);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        AlertDTO.Response response = alertService.getAlert(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(Alert.AlertStatus.OPEN);
        assertThat(response.getTransactionRef()).isEqualTo("TX-999");
    }

    @Test
    void lookingUpAnAlertThatNoLongerExistsThrowsAClearError() {
        when(alertRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.getAlert(404L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Alert not found: 404");
    }

    @Test
    void analystForwardsAnOpenAlertToTheInvestigationTeam() {
        Alert alert = anOpenAlert(10L);
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertDTO.ActionRequest request = new AlertDTO.ActionRequest("Suspicious pattern", "Investigation Team");
        AlertDTO.Response response = alertService.forwardAlert(10L, request, "analyst@bank.com");

        assertThat(response.getStatus()).isEqualTo(Alert.AlertStatus.FORWARDED);
        assertThat(response.getAssignedTo()).isEqualTo("Investigation Team");
        verify(auditTrailService).recordAction(any(), eq(AlertAuditTrail.AuditAction.FORWARDED), eq("analyst@bank.com"), any());
    }

    @Test
    void forwardingDefaultsAssigneeToInvestigationTeamWhenNoneIsProvided() {
        Alert alert = anOpenAlert(11L);
        when(alertRepository.findById(11L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertDTO.ActionRequest request = new AlertDTO.ActionRequest(null, null);
        AlertDTO.Response response = alertService.forwardAlert(11L, request, "system");

        assertThat(response.getAssignedTo()).isEqualTo("Investigation Team");
    }

    @Test
    void tryingToForwardAnAlertThatIsAlreadyForwardedIsRejected() {
        Alert alert = anOpenAlert(20L);
        alert.setStatus(Alert.AlertStatus.FORWARDED);
        when(alertRepository.findById(20L)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> alertService.forwardAlert(20L, new AlertDTO.ActionRequest(null, null), "analyst"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only forward OPEN alerts");
    }

    @Test
    void analystDismissesAFalsePositiveAlert() {
        Alert alert = anOpenAlert(30L);
        when(alertRepository.findById(30L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertDTO.ActionRequest request = new AlertDTO.ActionRequest("Verified as false positive", null);
        AlertDTO.Response response = alertService.dismissAlert(30L, request, "analyst@bank.com");

        assertThat(response.getStatus()).isEqualTo(Alert.AlertStatus.DISMISSED);
        verify(auditTrailService).recordAction(any(), eq(AlertAuditTrail.AuditAction.DISMISSED), eq("analyst@bank.com"), any());
    }

    @Test
    void dismissingAnAlertThatIsNotOpenIsRejected() {
        Alert alert = anOpenAlert(31L);
        alert.setStatus(Alert.AlertStatus.DISMISSED);
        when(alertRepository.findById(31L)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> alertService.dismissAlert(31L, new AlertDTO.ActionRequest(null, null), "analyst"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only dismiss OPEN alerts");
    }

    @Test
    void supervisorClosesAResolvedAlertAfterInvestigation() {
        Alert alert = anOpenAlert(40L);
        alert.setStatus(Alert.AlertStatus.FORWARDED);
        when(alertRepository.findById(40L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlertDTO.ActionRequest request = new AlertDTO.ActionRequest("Case resolved", null);
        AlertDTO.Response response = alertService.closeAlert(40L, request, "supervisor@bank.com");

        assertThat(response.getStatus()).isEqualTo(Alert.AlertStatus.CLOSED);
        verify(auditTrailService).recordAction(any(), eq(AlertAuditTrail.AuditAction.CLOSED), eq("supervisor@bank.com"), any());
    }

    @Test
    void closingAnAlreadyClosedAlertIsRejected() {
        Alert alert = anOpenAlert(41L);
        alert.setStatus(Alert.AlertStatus.CLOSED);
        when(alertRepository.findById(41L)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> alertService.closeAlert(41L, new AlertDTO.ActionRequest(null, null), "supervisor"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void dashboardStatsShowCorrectBreakdownAcrossAllAlertStatuses() {
        when(alertRepository.count()).thenReturn(100L);
        when(alertRepository.countByStatus(Alert.AlertStatus.OPEN)).thenReturn(40L);
        when(alertRepository.countByStatus(Alert.AlertStatus.FORWARDED)).thenReturn(30L);
        when(alertRepository.countByStatus(Alert.AlertStatus.DISMISSED)).thenReturn(20L);
        when(alertRepository.countByStatus(Alert.AlertStatus.CLOSED)).thenReturn(10L);
        when(alertRepository.countAlertsSince(any())).thenReturn(5L);

        AlertDTO.StatsResponse stats = alertService.getStats();

        assertThat(stats.getTotalAlerts()).isEqualTo(100L);
        assertThat(stats.getOpenAlerts()).isEqualTo(40L);
        assertThat(stats.getForwardedAlerts()).isEqualTo(30L);
        assertThat(stats.getDismissedAlerts()).isEqualTo(20L);
        assertThat(stats.getClosedAlerts()).isEqualTo(10L);
        assertThat(stats.getPercentageForwarded()).isEqualTo(30.0);
    }

    @Test
    void percentageForwardedIsZeroWhenThereAreNoAlertsAtAll() {
        when(alertRepository.count()).thenReturn(0L);
        when(alertRepository.countByStatus(any())).thenReturn(0L);
        when(alertRepository.countAlertsSince(any())).thenReturn(0L);

        AlertDTO.StatsResponse stats = alertService.getStats();

        assertThat(stats.getPercentageForwarded()).isZero();
    }

    @Test
    void auditHistoryForAnAlertComesBackInChronologicalOrder() {
        AlertAuditTrail entry = new AlertAuditTrail();
        entry.setId(1L);
        entry.setAction(AlertAuditTrail.AuditAction.CREATED);
        entry.setPerformedBy("SYSTEM");
        Alert parent = anOpenAlert(5L);
        entry.setAlert(parent);

        when(auditTrailRepository.findByAlertIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(entry));

        var history = alertService.getAuditTrail(5L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(AlertAuditTrail.AuditAction.CREATED);
        assertThat(history.get(0).getPerformedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void auditHistoryIsEmptyForABrandNewAlertWithNoActions() {
        when(auditTrailRepository.findByAlertIdOrderByCreatedAtAsc(99L)).thenReturn(List.of());

        var history = alertService.getAuditTrail(99L);

        assertThat(history).isEmpty();
    }

    @Test
    void investigationQueueShowsOnlyForwardedAlerts() {
        Alert forwarded = anOpenAlert(7L);
        forwarded.setStatus(Alert.AlertStatus.FORWARDED);
        when(alertRepository.findByStatus(eq(Alert.AlertStatus.FORWARDED), anyInt(), anyInt()))
                .thenReturn(List.of(forwarded));

        List<AlertDTO.Response> queue = alertService.getForwardedAlerts();

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).getStatus()).isEqualTo(Alert.AlertStatus.FORWARDED);
    }

    @Test
    void investigationQueueIsEmptyWhenNoAlertsHaveBeenForwardedYet() {
        when(alertRepository.findByStatus(eq(Alert.AlertStatus.FORWARDED), anyInt(), anyInt()))
                .thenReturn(List.of());

        assertThat(alertService.getForwardedAlerts()).isEmpty();
    }

    private Alert anOpenAlert(Long id) {
        Transaction tx = new Transaction();
        tx.setId(100L);
        tx.setTransactionRef("TX-999");
        tx.setAccountId("ACCT-42");
        tx.setCustomerName("Jane Smith");

        MonitoringRule rule = new MonitoringRule();
        rule.setId(1L);
        rule.setRuleCode("LARGE_TX");
        rule.setRuleName("Large Transaction");

        Alert alert = new Alert();
        alert.setId(id);
        alert.setStatus(Alert.AlertStatus.OPEN);
        alert.setSeverity(Alert.AlertSeverity.HIGH);
        alert.setDescription("Amount exceeds threshold");
        alert.setTransaction(tx);
        alert.setRule(rule);
        return alert;
    }
}

