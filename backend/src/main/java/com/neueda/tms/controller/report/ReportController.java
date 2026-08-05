package com.neueda.tms.controller.report;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.transaction.TransactionDTO;
import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.alert.AuditTrailDTO;
import com.neueda.tms.service.report.IReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for report generation.
 * All endpoints are restricted to authenticated ADMIN or ANALYST users.
 */
@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
public class ReportController {

    private final IReportService reportService;

    @Autowired
    public ReportController(IReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<TransactionDTO.Response>> transactionReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(reportService.getTransactionReport(fromDate, toDate, page, size));
    }

    @GetMapping("/alerts")
    public ResponseEntity<PageResponse<AlertDTO.Response>> alertReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(reportService.getAlertReport(fromDate, toDate, status, page, size));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> accountReport() {
        return ResponseEntity.ok(reportService.getAccountAlertReport());
    }

    @GetMapping("/audit")
    public ResponseEntity<PageResponse<AuditTrailDTO>> auditReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(reportService.getAuditReport(fromDate, toDate, action, page, size));
    }
}
