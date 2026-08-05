package com.neueda.tms.service.rule;

import com.neueda.tms.repository.alert.Alert;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.transaction.Transaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rule: RESTRICTED_COUNTRY
 * Triggered when a transaction originates from a restricted country.
 * Default restricted countries: ["KP"] (North Korea).
 * Configurable from dashboard (add/remove countries).
 */
@Component
public class RestrictedCountryRule implements RuleEvaluator {

    @Override
    public String getRuleCode() {
        return "RESTRICTED_COUNTRY";
    }

    @Override
    public Optional<String> evaluate(Transaction transaction, MonitoringRule rule) {
        List<String> restrictedCountries = getRestrictedCountries(rule);

        String countryCode = transaction.getCountryCode().toUpperCase();
        if (restrictedCountries.stream().anyMatch(c -> c.equalsIgnoreCase(countryCode))) {
            return Optional.of(String.format(
                "Transaction originates from restricted country: %s. Account: %s.",
                transaction.getCountryCode(),
                transaction.getAccountId()
            ));
        }
        return Optional.empty();
    }

    @Override
    public Alert.AlertSeverity getSeverity(MonitoringRule rule) {
        return Alert.AlertSeverity.CRITICAL;
    }

    @SuppressWarnings("unchecked")
    private List<String> getRestrictedCountries(MonitoringRule rule) {
        if (rule.getParameters() != null && rule.getParameters().containsKey("countries")) {
            Object countriesObj = rule.getParameters().get("countries");
            if (countriesObj instanceof List) {
                return (List<String>) countriesObj;
            }
        }
        // Default: North Korea
        List<String> defaults = new ArrayList<>();
        defaults.add("KP");
        return defaults;
    }
}
