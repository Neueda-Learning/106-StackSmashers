package com.neueda.tms.service.alert;

import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.alert.AuditTrailDTO;
import com.neueda.tms.controller.common.PageResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface IAlertService {

    PageResponse<AlertDTO.Response> searchAlerts(
            String status,
            String severity,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir);

    AlertDTO.Response getAlert(Long id);

    List<AuditTrailDTO> getAuditTrail(Long alertId);

    AlertDTO.Response forwardAlert(
            Long id,
            AlertDTO.ActionRequest request,
            String operator);

    AlertDTO.Response dismissAlert(
            Long id,
            AlertDTO.ActionRequest request,
            String operator);

    AlertDTO.Response closeAlert(
            Long id,
            AlertDTO.ActionRequest request,
            String operator);

    AlertDTO.StatsResponse getStats();

    List<AlertDTO.Response> getForwardedAlerts();
}