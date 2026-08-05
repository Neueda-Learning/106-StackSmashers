package com.neueda.tms.repository.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class Transaction {

    private Long id;
    private String transactionRef;
    private String accountId;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private String countryCode;
    private TransactionType transactionType;
    private TransactionStatus status = TransactionStatus.PENDING;
    private Boolean isNewCustomer = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private Map<String, Object> metadata;

    public Transaction() {
    }

    public Transaction(String transactionRef, String accountId, String customerName, BigDecimal amount, String currency, String countryCode, TransactionType transactionType, Map<String, Object> metadata) {
        this.transactionRef = transactionRef;
        this.accountId = accountId;
        this.customerName = customerName;
        this.amount = amount;
        this.currency = currency;
        this.countryCode = countryCode;
        this.transactionType = transactionType;
        this.metadata = metadata;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Boolean getIsNewCustomer() {
        return isNewCustomer;
    }

    public void setIsNewCustomer(Boolean isNewCustomer) {
        this.isNewCustomer = isNewCustomer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public enum TransactionType {
        CREDIT, DEBIT, TRANSFER
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FLAGGED
    }
}
