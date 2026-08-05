package com.neueda.tms.service.report;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.transaction.TransactionDTO;
import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.alert.AuditTrailDTO;
import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertAuditTrail;
import com.neueda.tms.repository.alert.AlertAuditTrailRepository;
import com.neueda.tms.repository.alert.AlertRepository;
import com.neueda.tms.repository.transaction.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReportService implements IReportService {

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final AlertAuditTrailRepository auditTrailRepository;

    @Autowired
    public ReportService(TransactionRepository transactionRepository,
                         AlertRepository alertRepository,
                         AlertAuditTrailRepository auditTrailRepository) {
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.auditTrailRepository = auditTrailRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionDTO.Response> getTransactionReport(
            LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {

        int offset = page * size;
        var content = transactionRepository.searchTransactions(
                null, null, null, null, fromDate, toDate, null, null,
                offset, size, "created_at", "desc");

        long totalElements = transactionRepository.countSearchTransactions(
                null, null, null, null, fromDate, toDate, null, null);

        long totalPages = (totalElements + size - 1) / size;

        return new PageResponse<>(
                content.stream().map(t -> new TransactionDTO.Response(
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
                        null, // metadata
                        0 // alertsGenerated
                )).toList(),
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
    public PageResponse<AlertDTO.Response> getAlertReport(
            LocalDateTime fromDate, LocalDateTime toDate, String status, int page, int size) {

        Alert.AlertStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = Alert.AlertStatus.valueOf(status.toUpperCase());
        }

        int offset = page * size;
        var content = alertRepository.searchAlerts(
                statusEnum, null, fromDate, toDate, null,
                offset, size, "created_at", "desc");

        long totalElements = alertRepository.countSearchAlerts(statusEnum, null, fromDate, toDate, null);
        long totalPages = (totalElements + size - 1) / size;

        return new PageResponse<>(
                content.stream().map(a -> new AlertDTO.Response(
                        a.getId(),
                        a.getTransaction().getId(),
                        a.getTransaction().getTransactionRef(),
                        a.getTransaction().getAccountId(),
                        a.getTransaction().getCustomerName(),
                        a.getRule().getId(),
                        a.getRule().getRuleCode(),
                        a.getRule().getRuleName(),
                        a.getStatus(),
                        a.getSeverity(),
                        a.getDescription(),
                        a.getAssignedTo(),
                        a.getCreatedAt(),
                        a.getUpdatedAt()
                )).toList(),
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
    public List<Map<String, Object>> getAccountAlertReport() {
        return alertRepository.countGroupByStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditTrailDTO> getAuditReport(
            LocalDateTime fromDate, LocalDateTime toDate, String action, int page, int size) {

        AlertAuditTrail.AuditAction actionEnum = null;
        if (action != null && !action.isBlank()) {
            actionEnum = AlertAuditTrail.AuditAction.valueOf(action.toUpperCase());
        }

        int offset = page * size;
        var content = auditTrailRepository.findAuditReport(fromDate, toDate, actionEnum, offset, size);
        long totalElements = auditTrailRepository.countAuditReport(fromDate, toDate, actionEnum);
        long totalPages = (totalElements + size - 1) / size;

        return new PageResponse<>(
                content.stream().map(t -> new AuditTrailDTO(
                        t.getId(),
                        t.getAlert().getId(),
                        t.getAlert().getTransaction().getId(),
                        t.getAlert().getTransaction().getTransactionRef(),
                        t.getAlert().getTransaction().getAccountId(),
                        t.getAction(),
                        t.getPerformedBy(),
                        t.getNotes(),
                        t.getCreatedAt()
                )).toList(),
                page,
                size,
                totalElements,
                (int) totalPages,
                page >= totalPages - 1,
                page == 0
        );
    }
}
