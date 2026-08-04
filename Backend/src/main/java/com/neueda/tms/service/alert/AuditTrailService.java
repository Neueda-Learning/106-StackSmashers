package com.neueda.tms.service.alert;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.alert.AlertAuditTrail;
import com.neueda.tms.repository.alert.AlertAuditTrailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTrailService {

    private static final Logger logger = LoggerFactory.getLogger(AuditTrailService.class);

    private final AlertAuditTrailRepository auditTrailRepository;

    @Autowired
    public AuditTrailService(AlertAuditTrailRepository auditTrailRepository) {
        this.auditTrailRepository = auditTrailRepository;
    }

    @Transactional
    public AlertAuditTrail recordAction(Alert alert,
            AlertAuditTrail.AuditAction action,
            String performedBy,
            String notes) {

        AlertAuditTrail auditTrail = new AlertAuditTrail(
                alert,
                action,
                performedBy,
                notes);

        AlertAuditTrail savedAudit = auditTrailRepository.save(auditTrail);

        logger.info(
                "Audit recorded - Alert: {}, Action: {}, By: {}",
                alert.getId(),
                action,
                performedBy);

        return savedAudit;
    }
}