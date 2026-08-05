package com.neueda.tms.controller.transaction;

import com.neueda.tms.repository.transaction.Transaction;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class TransactionDTO {

    public static class Request {

        @NotBlank(message = "Transaction reference is required")
        @Size(max = 64)
        private String transactionRef;

        @NotBlank(message = "Account ID is required")
        @Size(max = 64)
        private String accountId;

        @NotBlank(message = "Customer name is required")
        private String customerName;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        @Digits(integer = 16, fraction = 2)
        private BigDecimal amount;

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 10)
        private String currency;

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 10)
        private String countryCode;

        @NotNull(message = "Transaction type is required")
        private Transaction.TransactionType transactionType;

        private Boolean isNewCustomer;

        private Map<String, Object> metadata;

        public Request() {}

        public Request(String transactionRef, String accountId, String customerName, BigDecimal amount, String currency, String countryCode, Transaction.TransactionType transactionType, Boolean isNewCustomer, Map<String, Object> metadata) {
            this.transactionRef = transactionRef;
            this.accountId = accountId;
            this.customerName = customerName;
            this.amount = amount;
            this.currency = currency;
            this.countryCode = countryCode;
            this.transactionType = transactionType;
            this.isNewCustomer = isNewCustomer;
            this.metadata = metadata;
        }

        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public Transaction.TransactionType getTransactionType() { return transactionType; }
        public void setTransactionType(Transaction.TransactionType transactionType) { this.transactionType = transactionType; }
        public Boolean getIsNewCustomer() { return isNewCustomer; }
        public void setIsNewCustomer(Boolean isNewCustomer) { this.isNewCustomer = isNewCustomer; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    public static class Response {
        private Long id;
        private String transactionRef;
        private String accountId;
        private String customerName;
        private BigDecimal amount;
        private String currency;
        private String countryCode;
        private Transaction.TransactionType transactionType;
        private Transaction.TransactionStatus status;
        private Boolean isNewCustomer;
        private LocalDateTime createdAt;
        private Map<String, Object> metadata;
        private int alertsGenerated;

        public Response() {}

        public Response(Long id, String transactionRef, String accountId, String customerName, BigDecimal amount, String currency, String countryCode, Transaction.TransactionType transactionType, Transaction.TransactionStatus status, Boolean isNewCustomer, LocalDateTime createdAt, Map<String, Object> metadata, int alertsGenerated) {
            this.id = id;
            this.transactionRef = transactionRef;
            this.accountId = accountId;
            this.customerName = customerName;
            this.amount = amount;
            this.currency = currency;
            this.countryCode = countryCode;
            this.transactionType = transactionType;
            this.status = status;
            this.isNewCustomer = isNewCustomer;
            this.createdAt = createdAt;
            this.metadata = metadata;
            this.alertsGenerated = alertsGenerated;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTransactionRef() { return transactionRef; }
        public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public Transaction.TransactionType getTransactionType() { return transactionType; }
        public void setTransactionType(Transaction.TransactionType transactionType) { this.transactionType = transactionType; }
        public Transaction.TransactionStatus getStatus() { return status; }
        public void setStatus(Transaction.TransactionStatus status) { this.status = status; }
        public Boolean getIsNewCustomer() { return isNewCustomer; }
        public void setIsNewCustomer(Boolean isNewCustomer) { this.isNewCustomer = isNewCustomer; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public int getAlertsGenerated() { return alertsGenerated; }
        public void setAlertsGenerated(int alertsGenerated) { this.alertsGenerated = alertsGenerated; }
    }
}
