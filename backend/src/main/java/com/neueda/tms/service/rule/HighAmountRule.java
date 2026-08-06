package com.neueda.tms.service.rule;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.service.currency.CurrencyConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Rule: HIGH_AMOUNT
 * Triggered when a transaction amount exceeds a configurable threshold.
 * Default threshold: 10000 USD equivalent.
 *
 * Transactions can be denominated in many different currencies, but the
 * threshold is defined in USD, so the transaction amount is converted to its
 * USD equivalent (via the static exchange rate table) purely for this
 * comparison — the original Transaction entity/amount/currency is never
 * modified or persisted differently.
 */
@Component
public class HighAmountRule implements RuleEvaluator {

    private final CurrencyConversionService currencyConversionService;

    @Autowired
    public HighAmountRule(CurrencyConversionService currencyConversionService) {
        this.currencyConversionService = currencyConversionService;
    }

    @Override
    public String getRuleCode() {
        return "HIGH_AMOUNT";
    }

    @Override
    public Optional<String> evaluate(Transaction transaction, MonitoringRule rule) {
        BigDecimal threshold = getThreshold(rule);
        BigDecimal amountUsd = currencyConversionService.toUsd(transaction.getAmount(), transaction.getCurrency());

        if (amountUsd.compareTo(threshold) > 0) {
            return Optional.of(String.format(
                "Transaction amount %s %s (~%s USD) exceeds the high amount threshold of %s USD.",
                transaction.getCurrency(),
                transaction.getAmount().toPlainString(),
                amountUsd.toPlainString(),
                threshold.toPlainString()
            ));
        }
        return Optional.empty();
    }

    @Override
    public Alert.AlertSeverity getSeverity(MonitoringRule rule) {
        // Escalate severity based on how much threshold is exceeded
        return Alert.AlertSeverity.HIGH;
    }

    private BigDecimal getThreshold(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("threshold")) {
            return new BigDecimal(rule.getParameters().get("threshold").toString());
        }
        return new BigDecimal("10000");
    }
}
