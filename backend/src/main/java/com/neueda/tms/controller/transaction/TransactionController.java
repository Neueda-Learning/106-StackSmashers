package com.neueda.tms.controller.transaction;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.service.transaction.ITransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * REST controller for transaction operations.
 * POST /transactions is open for bank system integration.
 * All other endpoints require authentication.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final ITransactionService transactionService;

    @Autowired
    public TransactionController(ITransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** Open endpoint — bank systems submit transactions without auth token. */
    @PostMapping
    public ResponseEntity<TransactionDTO.Response> submit(
            @Valid @RequestBody TransactionDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.submitTransaction(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<TransactionDTO.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<PageResponse<TransactionDTO.Response>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(transactionService.searchTransactions(
                search, status, type, countryCode,
                fromDate, toDate, minAmount, maxAmount,
                page, size, sortBy, sortDir));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<PageResponse<TransactionDTO.Response>> getByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId, page, size));
    }
}
