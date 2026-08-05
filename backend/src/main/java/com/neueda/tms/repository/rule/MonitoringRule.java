package com.neueda.tms.repository.rule;

import java.util.Map;
import java.time.LocalDateTime;

public class MonitoringRule {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private String description;
    private RuleSeverity severity;
    private Boolean isActive = true;
    private Map<String, Object> parameters;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    public MonitoringRule() {
    }

    public MonitoringRule(String ruleCode, String ruleName, String description, RuleSeverity severity, Map<String, Object> parameters) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.description = description;
        this.severity = severity;
        this.parameters = parameters;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public RuleSeverity getSeverity() { return severity; }
    public void setSeverity(RuleSeverity severity) { this.severity = severity; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum RuleSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
