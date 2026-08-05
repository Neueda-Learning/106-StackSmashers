package com.neueda.tms.service.rule;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Optional;

/**
 * Rule: ODD_HOURS
 * Triggered when a transaction occurs during unusual hours.
 * Default: between 00:00 and 04:00 (midnight to 4am).
 * Configurable start and end hour from dashboard.
 */
@Component
public class OddHoursRule implements RuleEvaluator {

    @Override
    public String getRuleCode() {
        return "ODD_HOURS";
    }

    @Override
    public Optional<String> evaluate(Transaction transaction, MonitoringRule rule) {
        int startHour = getStartHour(rule);
        int endHour = getEndHour(rule);

        LocalTime txTime = transaction.getCreatedAt().toLocalTime();
        LocalTime start = LocalTime.of(startHour, 0);
        LocalTime end = LocalTime.of(endHour, 0);

        boolean isOddHour = txTime.isAfter(start.minusSeconds(1)) && txTime.isBefore(end.plusSeconds(1));
        if (isOddHour) {
            return Optional.of(String.format(
                "Transaction occurred at unusual time %s (suspicious window: %02d:00 - %02d:00).",
                txTime,
                startHour,
                endHour
            ));
        }
        return Optional.empty();
    }

    @Override
    public Alert.AlertSeverity getSeverity(MonitoringRule rule) {
        return Alert.AlertSeverity.LOW;
    }

    private int getStartHour(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("startHour")) {
            return ((Number) rule.getParameters().get("startHour")).intValue();
        }
        return 0;
    }

    private int getEndHour(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("endHour")) {
            return ((Number) rule.getParameters().get("endHour")).intValue();
        }
        return 4;
    }
}
