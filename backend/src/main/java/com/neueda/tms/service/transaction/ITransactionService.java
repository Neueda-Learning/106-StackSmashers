package com.neueda.tms.service.transaction;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.transaction.TransactionDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ITransactionService {

    TransactionDTO.Response submitTransaction(TransactionDTO.Request request);

    TransactionDTO.Response getTransaction(Long id);

    PageResponse<TransactionDTO.Response> searchTransactions(
            String search, String status, String type, String countryCode,
            LocalDateTime fromDate, LocalDateTime toDate,
            BigDecimal minAmount, BigDecimal maxAmount,
            int page, int size, String sortBy, String sortDir);

    PageResponse<TransactionDTO.Response> getTransactionsByAccount(
            String accountId, int page, int size);
}
