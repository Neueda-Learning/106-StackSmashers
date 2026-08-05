package com.neueda.tms.service.report;

import com.neueda.tms.controller.common.PageResponse;
import com.neueda.tms.controller.transaction.TransactionDTO;
import com.neueda.tms.controller.alert.AlertDTO;
import com.neueda.tms.controller.alert.AuditTrailDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IReportService {

    PageResponse<TransactionDTO.Response> getTransactionReport(
            LocalDateTime fromDate, LocalDateTime toDate, int page, int size);

    PageResponse<AlertDTO.Response> getAlertReport(
            LocalDateTime fromDate, LocalDateTime toDate, String status, int page, int size);

    List<Map<String, Object>> getAccountAlertReport();

    PageResponse<AuditTrailDTO> getAuditReport(
            LocalDateTime fromDate, LocalDateTime toDate, String action, int page, int size);
}
