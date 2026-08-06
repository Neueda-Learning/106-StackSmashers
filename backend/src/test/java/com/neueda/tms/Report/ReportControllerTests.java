package com.neueda.tms.Report;

import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.alert.AuditTrailDTO;
import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.report.ReportController;
import com.neueda.tms.controller.transaction.TransactionDTO;
import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertAuditTrail;
import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.service.report.IReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTests {

    @Mock
    private IReportService reportService;

    private ReportController reportController;

    @BeforeEach
    void setUp() {
        reportController = new ReportController(reportService);
    }

    @Test
    void transactionReportEndpointReturnsPagedTransactionsFromTheService() {
        TransactionDTO.Response tx = aTransactionResponse(1L);
        PageResponse<TransactionDTO.Response> page = new PageResponse<>(List.of(tx), 0, 50, 1, 1, true, true);
        when(reportService.getTransactionReport(null, null, 0, 50)).thenReturn(page);

        ResponseEntity<PageResponse<TransactionDTO.Response>> response =
                reportController.transactionReport(null, null, 0, 50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getTransactionRef()).isEqualTo("TX-1");
    }

    @Test
    void transactionReportReturnsAnEmptyPageWhenNoTransactionsMatchTheDateRange() {
        PageResponse<TransactionDTO.Response> emptyPage = new PageResponse<>(List.of(), 0, 50, 0, 0, true, true);
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(reportService.getTransactionReport(from, to, 0, 50)).thenReturn(emptyPage);

        ResponseEntity<PageResponse<TransactionDTO.Response>> response =
                reportController.transactionReport(from, to, 0, 50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void alertReportEndpointReturnsPagedAlertsFilteredByStatus() {
        AlertDTO.Response alert = anAlertResponse(5L, Alert.AlertStatus.OPEN);
        PageResponse<AlertDTO.Response> page = new PageResponse<>(List.of(alert), 0, 50, 1, 1, true, true);
        when(reportService.getAlertReport(null, null, "OPEN", 0, 50)).thenReturn(page);

        ResponseEntity<PageResponse<AlertDTO.Response>> response =
                reportController.alertReport(null, null, "OPEN", 0, 50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getStatus()).isEqualTo(Alert.AlertStatus.OPEN);
    }

    @Test
    void alertReportWithNoStatusFilterReturnsAllAlerts() {
        PageResponse<AlertDTO.Response> page = new PageResponse<>(
                List.of(anAlertResponse(1L, Alert.AlertStatus.OPEN), anAlertResponse(2L, Alert.AlertStatus.CLOSED)),
                0, 50, 2, 1, true, true);
        when(reportService.getAlertReport(null, null, null, 0, 50)).thenReturn(page);

        ResponseEntity<PageResponse<AlertDTO.Response>> response =
                reportController.alertReport(null, null, null, 0, 50);

        assertThat(response.getBody().getContent()).hasSize(2);
    }

    @Test
    void accountReportEndpointReturnsGroupedAlertCountsPerAccount() {
        List<Map<String, Object>> accountData = List.of(
                Map.of("status", "OPEN", "count", 4L),
                Map.of("status", "CLOSED", "count", 2L));
        when(reportService.getAccountAlertReport()).thenReturn(accountData);

        ResponseEntity<List<Map<String, Object>>> response = reportController.accountReport();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0)).containsEntry("status", "OPEN");
    }

    @Test
    void accountReportReturnsEmptyListWhenNoAlertDataExists() {
        when(reportService.getAccountAlertReport()).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> response = reportController.accountReport();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void auditReportEndpointReturnsPagedAuditEntriesFilteredByAction() {
        AuditTrailDTO entry = new AuditTrailDTO(1L, 2L, 3L, "TX-10", "ACCT-42",
                AlertAuditTrail.AuditAction.FORWARDED, "analyst@bank.com", "Sent to fraud team", LocalDateTime.now());
        PageResponse<AuditTrailDTO> page = new PageResponse<>(List.of(entry), 0, 50, 1, 1, true, true);
        when(reportService.getAuditReport(null, null, "FORWARDED", 0, 50)).thenReturn(page);

        ResponseEntity<PageResponse<AuditTrailDTO>> response =
                reportController.auditReport(null, null, "FORWARDED", 0, 50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getAction()).isEqualTo(AlertAuditTrail.AuditAction.FORWARDED);
        assertThat(response.getBody().getContent().get(0).getPerformedBy()).isEqualTo("analyst@bank.com");
    }

    @Test
    void auditReportReturnsAnEmptyPageWhenNothingMatchesTheGivenDateRange() {
        PageResponse<AuditTrailDTO> emptyPage = new PageResponse<>(List.of(), 0, 50, 0, 0, true, true);
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        when(reportService.getAuditReport(from, null, null, 0, 50)).thenReturn(emptyPage);

        ResponseEntity<PageResponse<AuditTrailDTO>> response =
                reportController.auditReport(from, null, null, 0, 50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEmpty();
    }

    private TransactionDTO.Response aTransactionResponse(Long id) {
        return new TransactionDTO.Response(id, "TX-" + id, "ACCT-42", "Jane Doe",
                new BigDecimal("250.00"), "USD", "US",
                Transaction.TransactionType.TRANSFER, Transaction.TransactionStatus.COMPLETED,
                false, LocalDateTime.now(), Map.of(), 0);
    }

    private AlertDTO.Response anAlertResponse(Long id, Alert.AlertStatus status) {
        AlertDTO.Response r = new AlertDTO.Response();
        r.setId(id);
        r.setStatus(status);
        r.setTransactionRef("TX-" + id);
        r.setSeverity(Alert.AlertSeverity.HIGH);
        return r;
    }
}

