package com.neueda.tms.controller.alert;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.service.alert.IAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final IAlertService alertService;

    @Autowired
    public AlertController(IAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<PageResponse<AlertDTO.Response>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(
                alertService.searchAlerts(
                        status,
                        severity,
                        fromDate,
                        toDate,
                        search,
                        page,
                        size,
                        sortBy,
                        sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<AlertDTO.Response> getAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlert(id));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<List<AuditTrailDTO>> getAuditTrail(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAuditTrail(id));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<AlertDTO.StatsResponse> getStats() {
        return ResponseEntity.ok(alertService.getStats());
    }

    @GetMapping("/investigation")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<List<AlertDTO.Response>> getForwardedAlerts() {
        return ResponseEntity.ok(alertService.getForwardedAlerts());
    }

    @PostMapping("/{id}/forward")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<AlertDTO.Response> forward(
            @PathVariable Long id,
            @RequestBody(required = false) AlertDTO.ActionRequest request,
            @AuthenticationPrincipal UserDetails operator) {

        return ResponseEntity.ok(
                alertService.forwardAlert(
                        id,
                        getRequest(request),
                        operator.getUsername()));
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<AlertDTO.Response> dismiss(
            @PathVariable Long id,
            @RequestBody(required = false) AlertDTO.ActionRequest request,
            @AuthenticationPrincipal UserDetails operator) {

        return ResponseEntity.ok(
                alertService.dismissAlert(
                        id,
                        getRequest(request),
                        operator.getUsername()));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<AlertDTO.Response> close(
            @PathVariable Long id,
            @RequestBody(required = false) AlertDTO.ActionRequest request,
            @AuthenticationPrincipal UserDetails operator) {

        return ResponseEntity.ok(
                alertService.closeAlert(
                        id,
                        getRequest(request),
                        operator.getUsername()));
    }

    private AlertDTO.ActionRequest getRequest(AlertDTO.ActionRequest request) {

        if (request == null) {
            return new AlertDTO.ActionRequest();
        }

        return request;
    }
}