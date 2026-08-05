package com.neueda.tms.service.rule;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Rule: ROUND_AMOUNT
 * Triggered when the transaction amount is a suspiciously round number.
 * This is a classic indicator of financial structuring (smurfing).
 * Default: amounts divisible by 1000.
 */
@Component
public class RoundAmountRule implements RuleEvaluator {

    @Override
    public String getRuleCode() {
        return "ROUND_AMOUNT";
    }

    @Override
    public Optional<String> evaluate(Transaction transaction, MonitoringRule rule) {
        BigDecimal divisor = getDivisor(rule);
        BigDecimal minAmount = getMinAmount(rule);

        // Only check amounts above the minimum to avoid false positives on small amounts
        if (transaction.getAmount().compareTo(minAmount) < 0) {
            return Optional.empty();
        }

        BigDecimal remainder = transaction.getAmount().remainder(divisor);
        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.of(String.format(
                "Transaction amount %s %s is a suspiciously round number (divisible by %s), possible structuring indicator.",
                transaction.getCurrency(),
                transaction.getAmount().toPlainString(),
                divisor.toPlainString()
            ));
        }
        return Optional.empty();
    }

    @Override
    public Alert.AlertSeverity getSeverity(MonitoringRule rule) {
        return Alert.AlertSeverity.MEDIUM;
    }

    private BigDecimal getDivisor(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("divisor")) {
            return new BigDecimal(rule.getParameters().get("divisor").toString());
        }
        return new BigDecimal("1000");
    }

    private BigDecimal getMinAmount(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("minAmount")) {
            return new BigDecimal(rule.getParameters().get("minAmount").toString());
        }
        return new BigDecimal("1000");
    }
}
