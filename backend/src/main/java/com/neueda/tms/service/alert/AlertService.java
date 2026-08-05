package com.neueda.tms.service.alert;

import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.alert.AuditTrailDTO;
import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertAuditTrail;
import com.neueda.tms.repository.alert.AlertAuditTrailRepository;
import com.neueda.tms.repository.alert.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AlertService implements IAlertService {

    private final AlertRepository alertRepository;
    private final AlertAuditTrailRepository auditTrailRepository;
    private final AuditTrailService auditTrailService;

    @Autowired
    public AlertService(AlertRepository alertRepository,
            AlertAuditTrailRepository auditTrailRepository,
            AuditTrailService auditTrailService) {

        this.alertRepository = alertRepository;
        this.auditTrailRepository = auditTrailRepository;
        this.auditTrailService = auditTrailService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AlertDTO.Response> searchAlerts(
            String status,
            String severity,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Alert.AlertStatus statusEnum = getStatus(status);
        Alert.AlertSeverity severityEnum = getSeverity(severity);

        int offset = page * size;

        List<Alert> alerts = alertRepository.searchAlerts(
                statusEnum,
                severityEnum,
                fromDate,
                toDate,
                search,
                offset,
                size,
                sortBy,
                sortDir);

        long total = alertRepository.countSearchAlerts(
                statusEnum,
                severityEnum,
                fromDate,
                toDate,
                search);

        int totalPages = (int) ((total + size - 1) / size);

        return new PageResponse<>(
                alerts.stream().map(this::toResponse).toList(),
                page,
                size,
                total,
                totalPages,
                page >= totalPages - 1,
                page == 0);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertDTO.Response getAlert(Long id) {
        return toResponse(findAlert(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditTrailDTO> getAuditTrail(Long alertId) {
        return auditTrailRepository
                .findByAlertIdOrderByCreatedAtAsc(alertId)
                .stream()
                .map(this::toAuditDto)
                .toList();
    }

    @Override
    @Transactional
    public AlertDTO.Response forwardAlert(Long id,
            AlertDTO.ActionRequest request,
            String operator) {

        Alert alert = findAlert(id);

        assertStatus(
                alert,
                Alert.AlertStatus.OPEN,
                "Can only forward OPEN alerts");

        alert.setStatus(Alert.AlertStatus.FORWARDED);
        alert.setAssignedTo(
                request.getAssignedTo() != null
                        ? request.getAssignedTo()
                        : "Investigation Team");

        Alert saved = saveAlert(alert);

        auditTrailService.recordAction(
                saved,
                AlertAuditTrail.AuditAction.FORWARDED,
                operator,
                getNotes(request, "Alert forwarded to investigation team."));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AlertDTO.Response dismissAlert(Long id,
            AlertDTO.ActionRequest request,
            String operator) {

        Alert alert = findAlert(id);

        assertStatus(
                alert,
                Alert.AlertStatus.OPEN,
                "Can only dismiss OPEN alerts");

        alert.setStatus(Alert.AlertStatus.DISMISSED);

        Alert saved = saveAlert(alert);

        auditTrailService.recordAction(
                saved,
                AlertAuditTrail.AuditAction.DISMISSED,
                operator,
                getNotes(request, "Alert dismissed."));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AlertDTO.Response closeAlert(Long id,
            AlertDTO.ActionRequest request,
            String operator) {

        Alert alert = findAlert(id);

        if (alert.getStatus() == Alert.AlertStatus.CLOSED) {
            throw new IllegalStateException("Alert is already closed.");
        }

        alert.setStatus(Alert.AlertStatus.CLOSED);

        Alert saved = saveAlert(alert);

        auditTrailService.recordAction(
                saved,
                AlertAuditTrail.AuditAction.CLOSED,
                operator,
                getNotes(request, "Alert closed."));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertDTO.StatsResponse getStats() {

        long total = alertRepository.count();
        long open = alertRepository.countByStatus(Alert.AlertStatus.OPEN);
        long forwarded = alertRepository.countByStatus(Alert.AlertStatus.FORWARDED);
        long dismissed = alertRepository.countByStatus(Alert.AlertStatus.DISMISSED);
        long closed = alertRepository.countByStatus(Alert.AlertStatus.CLOSED);

        double percentage = total == 0
                ? 0
                : Math.round(((double) forwarded / total) * 10000) / 100.0;

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        return new AlertDTO.StatsResponse(
                total,
                open,
                forwarded,
                dismissed,
                closed,
                percentage,
                alertRepository.countAlertsSince(last24Hours),
                alertRepository.countAlertsSince(last7Days));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDTO.Response> getForwardedAlerts() {

        return alertRepository
                .findByStatus(Alert.AlertStatus.FORWARDED, 0, 1000)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Alert saveAlert(Alert alert) {
        alert.setUpdatedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }

    private String getNotes(AlertDTO.ActionRequest request,
            String defaultMessage) {

        if (request.getNotes() == null) {
            return defaultMessage;
        }

        return request.getNotes();
    }

    private Alert.AlertStatus getStatus(String status) {

        if (status == null || status.isBlank()) {
            return null;
        }

        return Alert.AlertStatus.valueOf(status.toUpperCase());
    }

    private Alert.AlertSeverity getSeverity(String severity) {

        if (severity == null || severity.isBlank()) {
            return null;
        }

        return Alert.AlertSeverity.valueOf(severity.toUpperCase());
    }

    private Alert findAlert(Long id) {

        return alertRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + id));
    }

    private void assertStatus(Alert alert,
            Alert.AlertStatus required,
            String message) {

        if (alert.getStatus() != required) {
            throw new IllegalStateException(
                    message + ". Current status: " + alert.getStatus());
        }
    }

    private AlertDTO.Response toResponse(Alert alert) {

        return new AlertDTO.Response(
                alert.getId(),
                alert.getTransaction().getId(),
                alert.getTransaction().getTransactionRef(),
                alert.getTransaction().getAccountId(),
                alert.getTransaction().getCustomerName(),
                alert.getRule().getId(),
                alert.getRule().getRuleCode(),
                alert.getRule().getRuleName(),
                alert.getStatus(),
                alert.getSeverity(),
                alert.getDescription(),
                alert.getAssignedTo(),
                alert.getCreatedAt(),
                alert.getUpdatedAt());
    }

    private AuditTrailDTO toAuditDto(AlertAuditTrail trail) {

        return new AuditTrailDTO(
                trail.getId(),
                trail.getAlert().getId(),
                trail.getAlert().getTransaction().getId(),
                trail.getAlert().getTransaction().getTransactionRef(),
                trail.getAlert().getTransaction().getAccountId(),
                trail.getAction(),
                trail.getPerformedBy(),
                trail.getNotes(),
                trail.getCreatedAt());
    }
}