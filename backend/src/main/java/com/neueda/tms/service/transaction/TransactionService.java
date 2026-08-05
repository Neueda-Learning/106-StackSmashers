package com.neueda.tms.service.transaction;

import com.neueda.tms.service.rule.MonitoringEngineService;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.transaction.TransactionDTO;
import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.repository.transaction.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionService implements ITransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);


    private final TransactionRepository transactionRepository;
    private final MonitoringEngineService monitoringEngineService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              MonitoringEngineService monitoringEngineService) {
        this.transactionRepository = transactionRepository;
        this.monitoringEngineService = monitoringEngineService;
    }

    @Override
    @Transactional
    public TransactionDTO.Response submitTransaction(TransactionDTO.Request request) {
        if (transactionRepository.findByTransactionRef(request.getTransactionRef()).isPresent()) {
            throw new IllegalArgumentException(
                    "Transaction with ref '" + request.getTransactionRef() + "' already exists.");
        }

        Transaction transaction = new Transaction(
                request.getTransactionRef(),
                request.getAccountId(),
                request.getCustomerName(),
                request.getAmount(),
                request.getCurrency(),
                request.getCountryCode(),
                request.getTransactionType(),
                request.getMetadata()
        );
        transaction.setIsNewCustomer(request.getIsNewCustomer() != null ? request.getIsNewCustomer() : false);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction saved: {}", saved.getTransactionRef());

        // Synchronous rule evaluation immediately after save
        List<Alert> alerts = monitoringEngineService.evaluate(saved);

        // Update status based on alerts
        if (!alerts.isEmpty()) {
            saved.setStatus(Transaction.TransactionStatus.FLAGGED);
        } else {
            saved.setStatus(Transaction.TransactionStatus.COMPLETED);
        }
        transactionRepository.save(saved);

        return toResponse(saved, alerts.size());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDTO.Response getTransaction(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + id));
        return toResponse(tx, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionDTO.Response> searchTransactions(
            String search, String status, String type, String countryCode,
            LocalDateTime fromDate, LocalDateTime toDate,
            BigDecimal minAmount, BigDecimal maxAmount,
            int page, int size, String sortBy, String sortDir) {

        Transaction.TransactionStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = Transaction.TransactionStatus.valueOf(status.toUpperCase());
        }

        Transaction.TransactionType typeEnum = null;
        if (type != null && !type.isBlank()) {
            typeEnum = Transaction.TransactionType.valueOf(type.toUpperCase());
        }

        int offset = page * size;
        List<Transaction> content = transactionRepository.searchTransactions(
                search, statusEnum, typeEnum, countryCode,
                fromDate, toDate, minAmount, maxAmount,
                offset, size, sortBy, sortDir);

        long totalElements = transactionRepository.countSearchTransactions(
                search, statusEnum, typeEnum, countryCode,
                fromDate, toDate, minAmount, maxAmount);

        long totalPages = (totalElements + size - 1) / size;

        return new PageResponse<>(
                content.stream().map(t -> toResponse(t, 0)).toList(),
                page,
                size,
                totalElements,
                (int) totalPages,
                page >= totalPages - 1,
                page == 0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionDTO.Response> getTransactionsByAccount(
            String accountId, int page, int size) {

        int offset = page * size;
        List<Transaction> content = transactionRepository.findByAccountId(accountId, offset, size);
        long totalElements = transactionRepository.countByAccountId(accountId);
        long totalPages = (totalElements + size - 1) / size;

        return new PageResponse<>(
                content.stream().map(t -> toResponse(t, 0)).toList(),
                page,
                size,
                totalElements,
                (int) totalPages,
                page >= totalPages - 1,
                page == 0
        );
    }

    private TransactionDTO.Response toResponse(Transaction t, int alertsGenerated) {
        return new TransactionDTO.Response(
                t.getId(),
                t.getTransactionRef(),
                t.getAccountId(),
                t.getCustomerName(),
                t.getAmount(),
                t.getCurrency(),
                t.getCountryCode(),
                t.getTransactionType(),
                t.getStatus(),
                t.getIsNewCustomer(),
                t.getCreatedAt(),
                t.getMetadata(),
                alertsGenerated
        );
    }
}
