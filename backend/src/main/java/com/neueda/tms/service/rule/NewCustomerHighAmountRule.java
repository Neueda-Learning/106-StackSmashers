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
 * Rule: NEW_CUSTOMER_HIGH_AMOUNT
 * Triggered when a new customer performs a transaction above a configurable threshold.
 * Default threshold: 5000.
 *
 * The threshold is defined in USD, so the transaction amount is converted to
 * its USD equivalent (via the static exchange rate table) purely for this
 * comparison — the original Transaction entity is never modified.
 */
@Component
public class NewCustomerHighAmountRule implements RuleEvaluator {

    private final CurrencyConversionService currencyConversionService;

    @Autowired
    public NewCustomerHighAmountRule(CurrencyConversionService currencyConversionService) {
        this.currencyConversionService = currencyConversionService;
    }

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
        BigDecimal amountUsd = currencyConversionService.toUsd(transaction.getAmount(), transaction.getCurrency());

        if (amountUsd.compareTo(threshold) > 0) {
            return Optional.of(String.format(
                "New customer '%s' attempted a high-value transaction of %s %s (~%s USD, threshold: %s USD).",
                transaction.getCustomerName(),
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
        return Alert.AlertSeverity.HIGH;
    }

    private BigDecimal getThreshold(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("threshold")) {
            return new BigDecimal(rule.getParameters().get("threshold").toString());
        }
        return new BigDecimal("5000");
    }
}
