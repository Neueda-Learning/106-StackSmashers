package com.neueda.tms.controller.rule;

import com.neueda.tms.repository.rule.MonitoringRule;

import java.time.LocalDateTime;
import java.util.Map;

public class RuleDTO {

    public static class Response {
        private Long id;
        private String ruleCode;
        private String ruleName;
        private String description;
        private MonitoringRule.RuleSeverity severity;
        private Boolean isActive;
        private Map<String, Object> parameters;
        private LocalDateTime updatedAt;

        public Response() {}

        public Response(Long id, String ruleCode, String ruleName, String description, MonitoringRule.RuleSeverity severity, Boolean isActive, Map<String, Object> parameters, LocalDateTime updatedAt) {
            this.id = id;
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
            this.description = description;
            this.severity = severity;
            this.isActive = isActive;
            this.parameters = parameters;
            this.updatedAt = updatedAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRuleCode() { return ruleCode; }
        public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public MonitoringRule.RuleSeverity getSeverity() { return severity; }
        public void setSeverity(MonitoringRule.RuleSeverity severity) { this.severity = severity; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class UpdateRequest {
        private Boolean isActive;
        private Map<String, Object> parameters;

        public UpdateRequest() {}

        public UpdateRequest(Boolean isActive, Map<String, Object> parameters) {
            this.isActive = isActive;
            this.parameters = parameters;
        }

        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
}
