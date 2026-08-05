package com.neueda.tms.service.rule;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Rule: NEW_CUSTOMER_HIGH_AMOUNT
 * Triggered when a new customer performs a transaction above a configurable threshold.
 * Default threshold: 5000.
 */
@Component
public class NewCustomerHighAmountRule implements RuleEvaluator {

    @Override
    public String getRuleCode() {
        return "NEW_CUSTOMER_HIGH_AMOUNT";
    }

    @Override
    public Optional<String> evaluate(Transaction transaction, MonitoringRule rule) {
        if (!Boolean.TRUE.equals(transaction.getIsNewCustomer())) {
            return Optional.empty();
        }

        BigDecimal threshold = getThreshold(rule);
        if (transaction.getAmount().compareTo(threshold) > 0) {
            return Optional.of(String.format(
                "New customer '%s' attempted a high-value transaction of %s %s (threshold: %s).",
                transaction.getCustomerName(),
                transaction.getCurrency(),
                transaction.getAmount().toPlainString(),
                threshold.toPlainString()
            ));
        }
        return Optional.empty();
    }

    @Override
    public Alert.AlertSeverity getSeverity(MonitoringRule rule) {
        return Alert.AlertSeverity.HIGH;
    }

    private BigDecimal getThreshold(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("threshold")) {
            return new BigDecimal(rule.getParameters().get("threshold").toString());
        }
        return new BigDecimal("5000");
    }
}
