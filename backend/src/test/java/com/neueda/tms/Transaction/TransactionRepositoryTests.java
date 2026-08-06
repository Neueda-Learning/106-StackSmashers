package com.neueda.tms.Transaction;

import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionRepositoryTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository = new TransactionRepository(jdbcTemplate);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void lookingUpATransactionByItsIdReturnsTheExpectedRecord() {
        Transaction expected = aTransaction();
        expected.setId(1L);

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(expected);

        Optional<Transaction> found = transactionRepository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
        assertThat(found.get().getTransactionRef()).isEqualTo("TXN-001");
    }

    @Test
    void lookingUpATransactionByAnIdThatDoesNotExistReturnsEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(999L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<Transaction> found = transactionRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    // ── findByTransactionRef ──────────────────────────────────────────────────

    @Test
    void findingATransactionByItsReferenceCodeReturnsTheRightRecord() {
        Transaction expected = aTransaction();

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("TXN-001")))
                .thenReturn(expected);

        Optional<Transaction> found = transactionRepository.findByTransactionRef("TXN-001");

        assertThat(found).isPresent();
        assertThat(found.get().getTransactionRef()).isEqualTo("TXN-001");
        assertThat(found.get().getAccountId()).isEqualTo("ACCT-42");
    }

    @Test
    void searchingForATransactionRefThatNobodyEverSubmittedComesBackEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("GHOST-REF")))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<Transaction> found = transactionRepository.findByTransactionRef("GHOST-REF");

        assertThat(found).isEmpty();
    }

    // ── findByAccountId ───────────────────────────────────────────────────────

    @Test
    void retrievingTransactionsForAKnownAccountReturnsAllMatchingRows() {
        Transaction t1 = aTransaction();
        t1.setId(1L);
        Transaction t2 = aTransaction();
        t2.setId(2L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("ACCT-42"), eq(10), eq(0)))
                .thenReturn(List.of(t1, t2));

        List<Transaction> results = transactionRepository.findByAccountId("ACCT-42", 0, 10);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Transaction::getId).containsExactly(1L, 2L);
    }

    @Test
    void anAccountWithNoTransactionsYetReturnsAnEmptyPage() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("ACCT-NEW"), eq(10), eq(0)))
                .thenReturn(List.of());

        List<Transaction> results = transactionRepository.findByAccountId("ACCT-NEW", 0, 10);

        assertThat(results).isEmpty();
    }

    // ── countAll ──────────────────────────────────────────────────────────────

    @Test
    void theTotalTransactionCountReflectsWhatIsStoredInTheDatabase() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(250L);

        assertThat(transactionRepository.countAll()).isEqualTo(250L);
    }

    @Test
    void aNullCountFromTheDatabaseIsSafelyReturnedAsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(null);

        assertThat(transactionRepository.countAll()).isZero();
    }

    // ── countByAccountId ──────────────────────────────────────────────────────

    @Test
    void countingTransactionsForASpecificAccountGivesTheCorrectTotal() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("ACCT-42")))
                .thenReturn(17L);

        assertThat(transactionRepository.countByAccountId("ACCT-42")).isEqualTo(17L);
    }

    @Test
    void anAccountThatHasNeverTransactedShowsACountOfZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("ACCT-FRESH")))
                .thenReturn(0L);

        assertThat(transactionRepository.countByAccountId("ACCT-FRESH")).isZero();
    }

    // ── countTransactionsSince ────────────────────────────────────────────────

    @Test
    void transactionsSubmittedAfterAGivenMomentAreCountedCorrectly() {
        LocalDateTime since = LocalDateTime.now().minusHours(1);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any()))
                .thenReturn(8L);

        assertThat(transactionRepository.countTransactionsSince(since)).isEqualTo(8L);
    }

    @Test
    void whenNothingHappenedInTheGivenWindowTheCountIsZero() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(5);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any()))
                .thenReturn(0L);

        assertThat(transactionRepository.countTransactionsSince(since)).isZero();
    }

    // ── countRecentTransactionsByAccount ──────────────────────────────────────

    @Test
    void recentActivityForAnAccountIsCountedOverTheSpecifiedWindow() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("ACCT-42"), any()))
                .thenReturn(5L);

        assertThat(transactionRepository.countRecentTransactionsByAccount("ACCT-42", since)).isEqualTo(5L);
    }

    @Test
    void anAccountWithNoActivityInTheWindowReportsZeroRecentTransactions() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(10);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("ACCT-QUIET"), any()))
                .thenReturn(0L);

        assertThat(transactionRepository.countRecentTransactionsByAccount("ACCT-QUIET", since)).isZero();
    }

    // ── searchTransactions ────────────────────────────────────────────────────

    @Test
    void searchingWithNoFiltersReturnsTheRequestedPage() {
        Transaction t = aTransaction();
        t.setId(1L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(t));

        List<Transaction> results = transactionRepository.searchTransactions(
                null, null, null, null, null, null, null, null, 0, 10, null, "desc");

        assertThat(results).hasSize(1);
    }

    @Test
    void searchingByStatusNarrowsResultsToOnlyMatchingTransactions() {
        Transaction flagged = aTransaction();
        flagged.setStatus(Transaction.TransactionStatus.FLAGGED);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(flagged));

        List<Transaction> results = transactionRepository.searchTransactions(
                null, Transaction.TransactionStatus.FLAGGED, null, null, null, null, null, null, 0, 10, "status", "asc");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(Transaction.TransactionStatus.FLAGGED);
    }

    @Test
    void countingSearchResultsWithNoFiltersGivesTheTotalNumberOfTransactions() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(100L);

        long count = transactionRepository.countSearchTransactions(
                null, null, null, null, null, null, null, null);

        assertThat(count).isEqualTo(100L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Transaction aTransaction() {
        Transaction t = new Transaction();
        t.setTransactionRef("TXN-001");
        t.setAccountId("ACCT-42");
        t.setCustomerName("Jane Doe");
        t.setAmount(new BigDecimal("500.00"));
        t.setCurrency("USD");
        t.setCountryCode("US");
        t.setTransactionType(Transaction.TransactionType.TRANSFER);
        t.setStatus(Transaction.TransactionStatus.PENDING);
        t.setIsNewCustomer(false);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }
}

