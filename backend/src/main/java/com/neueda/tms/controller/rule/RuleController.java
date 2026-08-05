package com.neueda.tms.controller.rule;

import com.neueda.tms.service.rule.IRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for monitoring rule management.
 * Rule reads are accessible to ADMIN and ANALYST.
 * Rule updates are restricted to ADMIN only — changing detection rules is a sensitive operation.
 */
@RestController
@RequestMapping("/rules")
public class RuleController {

    private final IRuleService ruleService;

    @Autowired
    public RuleController(IRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<List<RuleDTO.Response>> getAllRules() {
        return ResponseEntity.ok(ruleService.getAllRules());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<RuleDTO.Response> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(ruleService.getRule(id));
    }

    /** Only administrators may modify detection rule parameters or activation status. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RuleDTO.Response> updateRule(
            @PathVariable Long id,
            @RequestBody RuleDTO.UpdateRequest request) {
        return ResponseEntity.ok(ruleService.updateRule(id, request));
    }
}
