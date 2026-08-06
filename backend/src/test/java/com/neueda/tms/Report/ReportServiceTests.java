package com.neueda.tms.Report;

import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.alert.AuditTrailDTO;
import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.transaction.TransactionDTO;
import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertAuditTrail;
import com.neueda.tms.repository.alert.AlertAuditTrailRepository;
import com.neueda.tms.repository.alert.AlertRepository;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.repository.transaction.TransactionRepository;
import com.neueda.tms.service.report.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertAuditTrailRepository auditTrailRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(transactionRepository, alertRepository, auditTrailRepository);
    }

    @Test
    void transactionReportMapsRepositoryRowsIntoResponseObjects() {
        when(transactionRepository.searchTransactions(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), eq("created_at"), eq("desc")))
                .thenReturn(List.of(aTransaction(1L)));
        when(transactionRepository.countSearchTransactions(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(1L);

        PageResponse<TransactionDTO.Response> report = reportService.getTransactionReport(null, null, 0, 20);

        assertThat(report.getContent()).hasSize(1);
        assertThat(report.getContent().get(0).getTransactionRef()).isEqualTo("TX-1");
        assertThat(report.getContent().get(0).getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
        assertThat(report.getTotalElements()).isEqualTo(1L);
        assertThat(report.isFirst()).isTrue();
    }

    @Test
    void transactionReportReturnsEmptyPageWhenNoneMatchTheDateWindow() {
        when(transactionRepository.searchTransactions(
                isNull(), isNull(), isNull(), isNull(), any(), any(), isNull(), isNull(),
                eq(0), eq(20), eq("created_at"), eq("desc")))
                .thenReturn(List.of());
        when(transactionRepository.countSearchTransactions(
                isNull(), isNull(), isNull(), isNull(), any(), any(), isNull(), isNull()))
                .thenReturn(0L);

        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        PageResponse<TransactionDTO.Response> report = reportService.getTransactionReport(from, to, 0, 20);

        assertThat(report.getContent()).isEmpty();
        assertThat(report.getTotalElements()).isZero();
    }

    @Test
    void alertReportFiltersAlertsByStatusBeforeReturningThePage() {
        when(alertRepository.searchAlerts(
                eq(Alert.AlertStatus.OPEN), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), anyString(), anyString()))
                .thenReturn(List.of(anAlert(2L, Alert.AlertStatus.OPEN)));
        when(alertRepository.countSearchAlerts(
                eq(Alert.AlertStatus.OPEN), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(1L);

        PageResponse<AlertDTO.Response> report = reportService.getAlertReport(null, null, "OPEN", 0, 20);

        assertThat(report.getContent()).hasSize(1);
        assertThat(report.getContent().get(0).getStatus()).isEqualTo(Alert.AlertStatus.OPEN);
    }

    @Test
    void alertReportWithNoStatusFilterReturnsAllAlerts() {
        when(alertRepository.searchAlerts(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(0), eq(20), anyString(), anyString()))
                .thenReturn(List.of(anAlert(3L, Alert.AlertStatus.OPEN), anAlert(4L, Alert.AlertStatus.CLOSED)));
        when(alertRepository.countSearchAlerts(
                isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(2L);

        PageResponse<AlertDTO.Response> report = reportService.getAlertReport(null, null, null, 0, 20);

        assertThat(report.getContent()).hasSize(2);
        assertThat(report.getTotalElements()).isEqualTo(2L);
    }

    @Test
    void accountAlertReportDelegatesToTheRepositoryGroupingAndReturnsItDirectly() {
        List<Map<String, Object>> grouped = List.of(
                Map.of("status", "OPEN", "count", 5L),
                Map.of("status", "CLOSED", "count", 3L));
        when(alertRepository.countGroupByStatus()).thenReturn(grouped);

        List<Map<String, Object>> result = reportService.getAccountAlertReport();

        assertThat(result).containsExactlyElementsOf(grouped);
    }

    @Test
    void auditReportFiltersEntriesByActionAndMapsThemIntoResponseObjects() {
        when(auditTrailRepository.findAuditReport(
                isNull(), isNull(), eq(AlertAuditTrail.AuditAction.FORWARDED), eq(0), eq(20)))
                .thenReturn(List.of(aTrailEntry(1L, AlertAuditTrail.AuditAction.FORWARDED)));
        when(auditTrailRepository.countAuditReport(
                isNull(), isNull(), eq(AlertAuditTrail.AuditAction.FORWARDED)))
                .thenReturn(1L);

        PageResponse<AuditTrailDTO> report = reportService.getAuditReport(null, null, "FORWARDED", 0, 20);

        assertThat(report.getContent()).hasSize(1);
        assertThat(report.getContent().get(0).getAction()).isEqualTo(AlertAuditTrail.AuditAction.FORWARDED);
        assertThat(report.getContent().get(0).getPerformedBy()).isEqualTo("analyst@bank.com");
    }

    @Test
    void auditReportWithNoActionFilterReturnAllAuditEntries() {
        when(auditTrailRepository.findAuditReport(isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(List.of(
                        aTrailEntry(1L, AlertAuditTrail.AuditAction.CREATED),
                        aTrailEntry(2L, AlertAuditTrail.AuditAction.DISMISSED)));
        when(auditTrailRepository.countAuditReport(isNull(), isNull(), isNull())).thenReturn(2L);

        PageResponse<AuditTrailDTO> report = reportService.getAuditReport(null, null, null, 0, 20);

        assertThat(report.getContent()).hasSize(2);
        assertThat(report.getTotalElements()).isEqualTo(2L);
    }

    private Transaction aTransaction(Long id) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setTransactionRef("TX-" + id);
        tx.setAccountId("ACCT-42");
        tx.setCustomerName("Jane Doe");
        tx.setAmount(new BigDecimal("100.00"));
        tx.setCurrency("USD");
        tx.setCountryCode("US");
        tx.setTransactionType(Transaction.TransactionType.TRANSFER);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setIsNewCustomer(false);
        tx.setCreatedAt(LocalDateTime.now());
        return tx;
    }

    private Alert anAlert(Long id, Alert.AlertStatus status) {
        Alert alert = new Alert();
        alert.setId(id);
        alert.setStatus(status);
        alert.setSeverity(Alert.AlertSeverity.HIGH);
        alert.setDescription("Threshold exceeded");
        alert.setTransaction(aTransaction(100L));
        MonitoringRule rule = new MonitoringRule();
        rule.setId(9L);
        rule.setRuleCode("HIGH_AMOUNT");
        rule.setRuleName("High Amount");
        alert.setRule(rule);
        alert.setCreatedAt(LocalDateTime.now());
        return alert;
    }

    private AlertAuditTrail aTrailEntry(Long id, AlertAuditTrail.AuditAction action) {
        AlertAuditTrail trail = new AlertAuditTrail();
        trail.setId(id);
        trail.setAction(action);
        trail.setPerformedBy("analyst@bank.com");
        trail.setNotes("Action notes");
        trail.setCreatedAt(LocalDateTime.now());
        trail.setAlert(anAlert(50L, Alert.AlertStatus.FORWARDED));
        return trail;
    }
}

