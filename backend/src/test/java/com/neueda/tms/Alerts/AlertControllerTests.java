package com.neueda.tms.Alerts;

import com.neueda.tms.controller.alert.AlertController;
import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.alert.AuditTrailDTO;
import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertAuditTrail;
import com.neueda.tms.service.alert.IAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertControllerTests {

    @Mock
    private IAlertService alertService;

    private AlertController alertController;
    private UserDetails analystUser;

    @BeforeEach
    void setUp() {
        alertController = new AlertController(alertService);
        analystUser = User.withUsername("analyst@bank.com").password("").roles("ANALYST").build();
    }

    @Test
    void fetchingAnAlertByIdReturnsItsFullDetails() {
        AlertDTO.Response alert = anAlertResponse(1L, Alert.AlertStatus.OPEN);
        when(alertService.getAlert(1L)).thenReturn(alert);

        ResponseEntity<AlertDTO.Response> response = alertController.getAlert(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getStatus()).isEqualTo(Alert.AlertStatus.OPEN);
    }

    @Test
    void requestingAnAlertThatDoesNotExistBubblesUpTheError() {
        when(alertService.getAlert(999L)).thenThrow(new NoSuchElementException("Alert not found: 999"));

        assertThatThrownBy(() -> alertController.getAlert(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("999");
    }

    @Test
    void searchingAlertsWithNoFiltersReturnsTheFirstPageOfResults() {
        PageResponse<AlertDTO.Response> page = aPageOf(List.of(anAlertResponse(1L, Alert.AlertStatus.OPEN)));
        when(alertService.searchAlerts(null, null, null, null, null, 0, 20, "createdAt", "desc"))
                .thenReturn(page);

        ResponseEntity<PageResponse<AlertDTO.Response>> response =
                alertController.search(null, null, null, null, null, 0, 20, "createdAt", "desc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void searchingAlertsFilteredByStatusOnlyShowsMatchingAlerts() {
        PageResponse<AlertDTO.Response> page = aPageOf(List.of(anAlertResponse(2L, Alert.AlertStatus.OPEN)));
        when(alertService.searchAlerts(eq("OPEN"), any(), any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(page);

        ResponseEntity<PageResponse<AlertDTO.Response>> response =
                alertController.search("OPEN", null, null, null, null, 0, 20, "createdAt", "desc");

        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getStatus()).isEqualTo(Alert.AlertStatus.OPEN);
    }

    @Test
    void searchingAlertsWithDateRangeForwardsThoseFiltersToTheService() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(alertService.searchAlerts(any(), any(), eq(from), eq(to), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(aPageOf(List.of()));

        ResponseEntity<PageResponse<AlertDTO.Response>> response =
                alertController.search(null, null, from, to, null, 0, 20, "createdAt", "desc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(alertService).searchAlerts(any(), any(), eq(from), eq(to), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void analystForwardsAnOpenAlertAndGetsBackTheUpdatedRecord() {
        AlertDTO.Response forwarded = anAlertResponse(5L, Alert.AlertStatus.FORWARDED);
        AlertDTO.ActionRequest request = new AlertDTO.ActionRequest("Suspicious activity", "Fraud Team");
        when(alertService.forwardAlert(eq(5L), any(), eq("analyst@bank.com"))).thenReturn(forwarded);

        ResponseEntity<AlertDTO.Response> response = alertController.forward(5L, request, analystUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(Alert.AlertStatus.FORWARDED);
    }

    @Test
    void forwardingWithNoRequestBodyStillWorksBecauseControllerCreatesADefault() {
        AlertDTO.Response forwarded = anAlertResponse(6L, Alert.AlertStatus.FORWARDED);
        when(alertService.forwardAlert(eq(6L), any(), eq("analyst@bank.com"))).thenReturn(forwarded);

        ResponseEntity<AlertDTO.Response> response = alertController.forward(6L, null, analystUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(Alert.AlertStatus.FORWARDED);
    }

    @Test
    void analystDismissesAFalsePositiveAndGetsBackTheDismissedRecord() {
        AlertDTO.Response dismissed = anAlertResponse(7L, Alert.AlertStatus.DISMISSED);
        AlertDTO.ActionRequest request = new AlertDTO.ActionRequest("Verified legitimate transaction", null);
        when(alertService.dismissAlert(eq(7L), any(), eq("analyst@bank.com"))).thenReturn(dismissed);

        ResponseEntity<AlertDTO.Response> response = alertController.dismiss(7L, request, analystUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(Alert.AlertStatus.DISMISSED);
    }

    @Test
    void supervisorClosesAnInvestigatedAlertAndReceivesConfirmation() {
        AlertDTO.Response closed = anAlertResponse(8L, Alert.AlertStatus.CLOSED);
        AlertDTO.ActionRequest request = new AlertDTO.ActionRequest("Case fully resolved", null);
        when(alertService.closeAlert(eq(8L), any(), eq("analyst@bank.com"))).thenReturn(closed);

        ResponseEntity<AlertDTO.Response> response = alertController.close(8L, request, analystUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(Alert.AlertStatus.CLOSED);
    }

    @Test
    void closingWithNoRequestBodyIsHandledGracefullyByTheController() {
        AlertDTO.Response closed = anAlertResponse(9L, Alert.AlertStatus.CLOSED);
        when(alertService.closeAlert(eq(9L), any(), eq("analyst@bank.com"))).thenReturn(closed);

        ResponseEntity<AlertDTO.Response> response = alertController.close(9L, null, analystUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void retrievingAlertStatsGivesACompleteBreakdownForTheDashboard() {
        AlertDTO.StatsResponse stats = new AlertDTO.StatsResponse(100L, 40L, 30L, 20L, 10L, 30.0, 5L, 25L);
        when(alertService.getStats()).thenReturn(stats);

        ResponseEntity<AlertDTO.StatsResponse> response = alertController.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalAlerts()).isEqualTo(100L);
        assertThat(response.getBody().getOpenAlerts()).isEqualTo(40L);
        assertThat(response.getBody().getPercentageForwarded()).isEqualTo(30.0);
    }

    @Test
    void theInvestigationQueueReturnsAllAlertsThatHaveBeenForwarded() {
        List<AlertDTO.Response> forwarded = List.of(
                anAlertResponse(10L, Alert.AlertStatus.FORWARDED),
                anAlertResponse(11L, Alert.AlertStatus.FORWARDED));
        when(alertService.getForwardedAlerts()).thenReturn(forwarded);

        ResponseEntity<List<AlertDTO.Response>> response = alertController.getForwardedAlerts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(a -> a.getStatus() == Alert.AlertStatus.FORWARDED);
    }

    @Test
    void theInvestigationQueueIsEmptyWhenNoAlertsHaveBeenForwardedYet() {
        when(alertService.getForwardedAlerts()).thenReturn(List.of());

        ResponseEntity<List<AlertDTO.Response>> response = alertController.getForwardedAlerts();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void auditTrailForAnAlertShowsEveryActionTakenOnIt() {
        AuditTrailDTO entry = new AuditTrailDTO(1L, 5L, 100L, "TX-001", "ACCT-42",
                AlertAuditTrail.AuditAction.FORWARDED, "analyst@bank.com", "Forwarded to fraud team", LocalDateTime.now());
        when(alertService.getAuditTrail(5L)).thenReturn(List.of(entry));

        ResponseEntity<List<AuditTrailDTO>> response = alertController.getAuditTrail(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getAction()).isEqualTo(AlertAuditTrail.AuditAction.FORWARDED);
        assertThat(response.getBody().get(0).getPerformedBy()).isEqualTo("analyst@bank.com");
    }

    @Test
    void auditTrailIsEmptyForAnAlertThatHasHadNoManualActions() {
        when(alertService.getAuditTrail(20L)).thenReturn(List.of());

        ResponseEntity<List<AuditTrailDTO>> response = alertController.getAuditTrail(20L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    private AlertDTO.Response anAlertResponse(Long id, Alert.AlertStatus status) {
        AlertDTO.Response r = new AlertDTO.Response();
        r.setId(id);
        r.setStatus(status);
        r.setTransactionRef("TX-" + id);
        r.setAccountId("ACCT-42");
        r.setCustomerName("Jane Smith");
        r.setSeverity(Alert.AlertSeverity.HIGH);
        return r;
    }

    private PageResponse<AlertDTO.Response> aPageOf(List<AlertDTO.Response> items) {
        return new PageResponse<>(items, 0, 20, items.size(), 1, true, true);
    }
}

