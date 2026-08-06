package com.neueda.tms.Transaction;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.transaction.TransactionController;
import com.neueda.tms.controller.transaction.TransactionDTO;
import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.service.transaction.ITransactionService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTests {

    @Mock
    private ITransactionService transactionService;

    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        transactionController = new TransactionController(transactionService);
    }

    @Test
    void submitTransactionReturns201WithTheResponseFromTheService() {
        TransactionDTO.Request request = new TransactionDTO.Request(
                "TXN-001", "ACC-123", "John Doe",
                new BigDecimal("500.00"), "USD", "US",
                Transaction.TransactionType.CREDIT, false, null);
        TransactionDTO.Response response = new TransactionDTO.Response(
                1L, "TXN-001", "ACC-123", "John Doe",
                new BigDecimal("500.00"), "USD", "US",
                Transaction.TransactionType.CREDIT, Transaction.TransactionStatus.PENDING,
                false, LocalDateTime.now(), null, 0);
        when(transactionService.submitTransaction(request)).thenReturn(response);

        ResponseEntity<TransactionDTO.Response> result = transactionController.submit(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getTransactionRef()).isEqualTo("TXN-001");
        assertThat(result.getBody().getAccountId()).isEqualTo("ACC-123");
        assertThat(result.getBody().getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void submitTransactionReturns201WhenAlertsAreGeneratedForTheFlaggedTransaction() {
        TransactionDTO.Request request = new TransactionDTO.Request(
                "TXN-002", "ACC-999", "Suspicious Actor",
                new BigDecimal("99999.99"), "USD", "IR",
                Transaction.TransactionType.TRANSFER, true, null);
        TransactionDTO.Response response = new TransactionDTO.Response(
                2L, "TXN-002", "ACC-999", "Suspicious Actor",
                new BigDecimal("99999.99"), "USD", "IR",
                Transaction.TransactionType.TRANSFER, Transaction.TransactionStatus.FLAGGED,
                true, LocalDateTime.now(), null, 3);
        when(transactionService.submitTransaction(request)).thenReturn(response);

        ResponseEntity<TransactionDTO.Response> result = transactionController.submit(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(Transaction.TransactionStatus.FLAGGED);
        assertThat(result.getBody().getAlertsGenerated()).isEqualTo(3);
    }

    @Test
    void getByIdReturnsTheTransactionWhenItExists() {
        TransactionDTO.Response response = new TransactionDTO.Response(
                42L, "TXN-042", "ACC-010", "Jane Smith",
                new BigDecimal("250.00"), "EUR", "DE",
                Transaction.TransactionType.DEBIT, Transaction.TransactionStatus.COMPLETED,
                false, LocalDateTime.now(), null, 0);
        when(transactionService.getTransaction(42L)).thenReturn(response);

        ResponseEntity<TransactionDTO.Response> result = transactionController.getById(42L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(42L);
        assertThat(result.getBody().getTransactionRef()).isEqualTo("TXN-042");
        assertThat(result.getBody().getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
    }

    @Test
    void searchReturnsPageOfTransactionsMatchingTheGivenFilters() {
        TransactionDTO.Response tx = new TransactionDTO.Response(
                1L, "TXN-001", "ACC-123", "John Doe",
                new BigDecimal("100.00"), "USD", "US",
                Transaction.TransactionType.CREDIT, Transaction.TransactionStatus.PENDING,
                false, LocalDateTime.now(), null, 0);
        PageResponse<TransactionDTO.Response> page = new PageResponse<>(
                List.of(tx), 0, 20, 1L, 1, true, true);
        when(transactionService.searchTransactions(
                any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        ResponseEntity<PageResponse<TransactionDTO.Response>> result =
                transactionController.search(null, "PENDING", "CREDIT", "US",
                        null, null, null, null, 0, 20, "createdAt", "desc");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getTotalElements()).isEqualTo(1L);
        assertThat(result.getBody().isFirst()).isTrue();
    }

    @Test
    void searchReturnsEmptyPageWhenNoTransactionsMatchTheFilters() {
        PageResponse<TransactionDTO.Response> emptyPage = new PageResponse<>(
                List.of(), 0, 20, 0L, 0, true, true);
        when(transactionService.searchTransactions(
                any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(emptyPage);

        ResponseEntity<PageResponse<TransactionDTO.Response>> result =
                transactionController.search("nonexistent", null, null, null,
                        null, null, null, null, 0, 20, "createdAt", "desc");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).isEmpty();
        assertThat(result.getBody().getTotalElements()).isZero();
    }

    @Test
    void getByAccountReturnsPageOfTransactionsForTheGivenAccount() {
        TransactionDTO.Response tx1 = new TransactionDTO.Response(
                1L, "TXN-001", "ACC-555", "Alice",
                new BigDecimal("300.00"), "GBP", "GB",
                Transaction.TransactionType.DEBIT, Transaction.TransactionStatus.COMPLETED,
                false, LocalDateTime.now(), null, 0);
        TransactionDTO.Response tx2 = new TransactionDTO.Response(
                2L, "TXN-002", "ACC-555", "Alice",
                new BigDecimal("150.00"), "GBP", "GB",
                Transaction.TransactionType.CREDIT, Transaction.TransactionStatus.PENDING,
                false, LocalDateTime.now(), null, 0);
        PageResponse<TransactionDTO.Response> page = new PageResponse<>(
                List.of(tx1, tx2), 0, 20, 2L, 1, true, true);
        when(transactionService.getTransactionsByAccount("ACC-555", 0, 20)).thenReturn(page);

        ResponseEntity<PageResponse<TransactionDTO.Response>> result =
                transactionController.getByAccount("ACC-555", 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(2);
        assertThat(result.getBody().getContent())
                .allMatch(t -> t.getAccountId().equals("ACC-555"));
    }

    @Test
    void getByAccountReturnsEmptyPageWhenAccountHasNoTransactions() {
        PageResponse<TransactionDTO.Response> emptyPage = new PageResponse<>(
                List.of(), 0, 20, 0L, 0, true, true);
        when(transactionService.getTransactionsByAccount("ACC-NEW", 0, 20)).thenReturn(emptyPage);

        ResponseEntity<PageResponse<TransactionDTO.Response>> result =
                transactionController.getByAccount("ACC-NEW", 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).isEmpty();
        assertThat(result.getBody().getTotalElements()).isZero();
    }
}
