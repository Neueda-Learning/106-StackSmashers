package com.neueda.tms.service.common;

import com.neueda.tms.repository.auth.User;
import com.neueda.tms.repository.auth.UserRepository;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.rule.MonitoringRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MonitoringRuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository,
            MonitoringRuleRepository ruleRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.ruleRepository = ruleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createAdminUser();
        createMonitoringRules();
    }

    private void createAdminUser() {

        if (userRepository.existsByUsername("admin")) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(User.UserRole.ADMIN);
        admin.setIsActive(true);

        userRepository.save(admin);

        System.out.println("Default admin user created.");
    }

    private void createMonitoringRules() {

        List<MonitoringRule> rules = List.of(

                createRule(
                        "HIGH_AMOUNT",
                        "High Transaction Amount",
                        "Triggered when transaction amount exceeds the threshold.",
                        MonitoringRule.RuleSeverity.HIGH,
                        Map.of("threshold", 10000)),

                createRule(
                        "RAPID_TRANSACTIONS",
                        "Rapid Transactions",
                        "Triggered when same account makes multiple transactions in a short window.",
                        MonitoringRule.RuleSeverity.CRITICAL,
                        Map.of(
                                "maxCount", 5,
                                "windowMinutes", 10)),

                createRule(
                        "RESTRICTED_COUNTRY",
                        "Restricted Country",
                        "Triggered when transaction originates from a restricted country.",
                        MonitoringRule.RuleSeverity.CRITICAL,
                        Map.of("countries", List.of("KP"))),

                createRule(
                        "NEW_CUSTOMER_HIGH_AMOUNT",
                        "New Customer High Amount",
                        "Triggered when a new customer makes a high-value transaction.",
                        MonitoringRule.RuleSeverity.HIGH,
                        Map.of("threshold", 5000)),

                createRule(
                        "ROUND_AMOUNT",
                        "Round Amount (Structuring)",
                        "Triggered when transaction amount is suspiciously round.",
                        MonitoringRule.RuleSeverity.MEDIUM,
                        Map.of(
                                "divisor", 1000,
                                "minAmount", 1000)),

                createRule(
                        "ODD_HOURS",
                        "Odd Hours Transaction",
                        "Triggered when a transaction occurs during unusual hours.",
                        MonitoringRule.RuleSeverity.LOW,
                        Map.of(
                                "startHour", 0,
                                "endHour", 4)));

        for (MonitoringRule rule : rules) {

            if (!ruleRepository.existsByRuleCode(rule.getRuleCode())) {

                ruleRepository.save(rule);

                System.out.println("Added Rule : " + rule.getRuleCode());
            }
        }
    }

    private MonitoringRule createRule(String code,
            String name,
            String description,
            MonitoringRule.RuleSeverity severity,
            Map<String, Object> parameters) {

        MonitoringRule rule = new MonitoringRule();

        rule.setRuleCode(code);
        rule.setRuleName(name);
        rule.setDescription(description);
        rule.setSeverity(severity);
        rule.setIsActive(true);
        rule.setParameters(new HashMap<>(parameters));

        return rule;
    }
}