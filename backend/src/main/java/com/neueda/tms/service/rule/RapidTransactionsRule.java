package com.neueda.tms.service.rule;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import com.neueda.tms.repository.transaction.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Rule: RAPID_TRANSACTIONS
 * Triggered when more than N transactions occur from the same account within M minutes.
 * Default: > 5 transactions in 10 minutes.
 */
@Component
public class RapidTransactionsRule implements RuleEvaluator {

    private final TransactionRepository transactionRepository;

    @Autowired
    public RapidTransactionsRule(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String getRuleCode() {
        return "RAPID_TRANSACTIONS";
    }

    @Override
    public Optional<String> evaluate(Transaction transaction, MonitoringRule rule) {
        int maxCount = getMaxCount(rule);
        int windowMinutes = getWindowMinutes(rule);

        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        long count = transactionRepository.countRecentTransactionsByAccount(
                transaction.getAccountId(), since
        );

        // Include the current transaction (not yet persisted in some flows)
        if (count >= maxCount) {
            return Optional.of(String.format(
                "Account '%s' has %d transactions in the last %d minutes (threshold: %d).",
                transaction.getAccountId(), count + 1, windowMinutes, maxCount
            ));
        }
        return Optional.empty();
    }

    @Override
    public Alert.AlertSeverity getSeverity(MonitoringRule rule) {
        return Alert.AlertSeverity.CRITICAL;
    }

    private int getMaxCount(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("maxCount")) {
            return ((Number) rule.getParameters().get("maxCount")).intValue();
        }
        return 5;
    }

    private int getWindowMinutes(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("windowMinutes")) {
            return ((Number) rule.getParameters().get("windowMinutes")).intValue();
        }
        return 10;
    }
}
