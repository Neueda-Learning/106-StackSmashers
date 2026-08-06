package com.neueda.tms.Transaction;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.transaction.TransactionDTO;
import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.repository.transaction.TransactionRepository;
import com.neueda.tms.service.rule.MonitoringEngineService;
import com.neueda.tms.service.transaction.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MonitoringEngineService monitoringEngineService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, monitoringEngineService);
    }

    // ── submitTransaction ─────────────────────────────────────────────────────

    @Test
    void aCleanTransactionThatBreaksNoRulesIsMarkedAsCompleted() {
        when(transactionRepository.findByTransactionRef("TXN-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        when(monitoringEngineService.evaluate(any())).thenReturn(List.of());

        TransactionDTO.Response response = transactionService.submitTransaction(aRequest());

        assertThat(response.getTransactionRef()).isEqualTo("TXN-001");
        assertThat(response.getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
        assertThat(response.getAlertsGenerated()).isZero();
    }

    @Test
    void aTransactionThatTriggersRulesIsMarkedAsFlaggedAndReportsAlertCount() {
        when(transactionRepository.findByTransactionRef("TXN-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });
        Alert alert = new Alert();
        when(monitoringEngineService.evaluate(any())).thenReturn(List.of(alert, alert));

        TransactionDTO.Response response = transactionService.submitTransaction(aRequest());

        assertThat(response.getStatus()).isEqualTo(Transaction.TransactionStatus.FLAGGED);
        assertThat(response.getAlertsGenerated()).isEqualTo(2);
    }

    @Test
    void submittingADuplicateTransactionRefIsRejectedWithAMeaningfulError() {
        Transaction existing = aTransaction();
        when(transactionRepository.findByTransactionRef("TXN-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> transactionService.submitTransaction(aRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TXN-001");
    }

    @Test
    void aNewCustomerFlagOnTheRequestIsPreservedInTheSavedTransaction() {
        TransactionDTO.Request req = aRequest();
        req.setIsNewCustomer(true);

        when(transactionRepository.findByTransactionRef("TXN-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(3L);
            return t;
        });
        when(monitoringEngineService.evaluate(any())).thenReturn(List.of());

        TransactionDTO.Response response = transactionService.submitTransaction(req);

        assertThat(response.getIsNewCustomer()).isTrue();
    }

    @Test
    void whenIsNewCustomerIsNullItDefaultsToFalseOnSubmit() {
        TransactionDTO.Request req = aRequest();
        req.setIsNewCustomer(null);

        when(transactionRepository.findByTransactionRef("TXN-001")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(4L);
            return t;
        });
        when(monitoringEngineService.evaluate(any())).thenReturn(List.of());

        TransactionDTO.Response response = transactionService.submitTransaction(req);

        assertThat(response.getIsNewCustomer()).isFalse();
    }

    // ── getTransaction ────────────────────────────────────────────────────────

    @Test
    void fetchingATransactionByIdReturnsItsFullDetails() {
        Transaction stored = aTransaction();
        stored.setId(10L);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(stored));

        TransactionDTO.Response response = transactionService.getTransaction(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTransactionRef()).isEqualTo("TXN-001");
        assertThat(response.getAccountId()).isEqualTo("ACCT-42");
    }

    @Test
    void requestingATransactionByAnIdThatDoesNotExistThrowsAnError() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("999");
    }

    // ── searchTransactions ────────────────────────────────────────────────────

    @Test
    void searchingWithNoFiltersReturnsAPaginatedViewOfAllTransactions() {
        Transaction t = aTransaction();
        t.setId(1L);
        when(transactionRepository.searchTransactions(any(), any(), any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class), any(), any()))
                .thenReturn(List.of(t));
        when(transactionRepository.countSearchTransactions(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        PageResponse<TransactionDTO.Response> page = transactionService.searchTransactions(
                null, null, null, null, null, null, null, null, 0, 10, "created_at", "desc");

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void searchingByStatusFiltersResultsToOnlyThatStatus() {
        Transaction flagged = aTransaction();
        flagged.setId(5L);
        flagged.setStatus(Transaction.TransactionStatus.FLAGGED);

        when(transactionRepository.searchTransactions(any(), any(), any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class), any(), any()))
                .thenReturn(List.of(flagged));
        when(transactionRepository.countSearchTransactions(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        PageResponse<TransactionDTO.Response> page = transactionService.searchTransactions(
                null, "FLAGGED", null, null, null, null, null, null, 0, 10, null, "desc");

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(Transaction.TransactionStatus.FLAGGED);
    }

    @Test
    void passingAnInvalidStatusStringThrowsAnIllegalArgumentException() {
        assertThatThrownBy(() -> transactionService.searchTransactions(
                null, "NOT_A_STATUS", null, null, null, null, null, null, 0, 10, null, "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void secondPageOfResultsIsCorrectlyMarkedAsNotFirst() {
        Transaction t = aTransaction();
        t.setId(1L);
        when(transactionRepository.searchTransactions(any(), any(), any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class), any(), any()))
                .thenReturn(List.of(t));
        when(transactionRepository.countSearchTransactions(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(25L);

        PageResponse<TransactionDTO.Response> page = transactionService.searchTransactions(
                null, null, null, null, null, null, null, null, 1, 10, null, "asc");

        assertThat(page.isFirst()).isFalse();
        assertThat(page.getPageNumber()).isEqualTo(1);
    }

    // ── getTransactionsByAccount ──────────────────────────────────────────────

    @Test
    void retrievingTransactionsForAnAccountWrapsThemInAPagedResponse() {
        Transaction t1 = aTransaction();
        t1.setId(1L);
        Transaction t2 = aTransaction();
        t2.setId(2L);

        when(transactionRepository.findByAccountId("ACCT-42", 0, 10)).thenReturn(List.of(t1, t2));
        when(transactionRepository.countByAccountId("ACCT-42")).thenReturn(2L);

        PageResponse<TransactionDTO.Response> page = transactionService.getTransactionsByAccount("ACCT-42", 0, 10);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2L);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void anAccountWithNoTransactionsReturnsAnEmptyPage() {
        when(transactionRepository.findByAccountId("ACCT-EMPTY", 0, 10)).thenReturn(List.of());
        when(transactionRepository.countByAccountId("ACCT-EMPTY")).thenReturn(0L);

        PageResponse<TransactionDTO.Response> page = transactionService.getTransactionsByAccount("ACCT-EMPTY", 0, 10);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TransactionDTO.Request aRequest() {
        return new TransactionDTO.Request(
                "TXN-001", "ACCT-42", "Jane Doe",
                new BigDecimal("500.00"), "USD", "US",
                Transaction.TransactionType.TRANSFER, false, null
        );
    }

    private Transaction aTransaction() {
        Transaction t = new Transaction();
        t.setTransactionRef("TXN-001");
        t.setAccountId("ACCT-42");
        t.setCustomerName("Jane Doe");
        t.setAmount(new BigDecimal("500.00"));
        t.setCurrency("USD");
        t.setCountryCode("US");
        t.setTransactionType(Transaction.TransactionType.TRANSFER);
        t.setStatus(Transaction.TransactionStatus.COMPLETED);
        t.setIsNewCustomer(false);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }
}
