package com.neueda.tms.Common;

import com.neueda.tms.repository.auth.User;
import com.neueda.tms.repository.auth.UserRepository;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.rule.MonitoringRuleRepository;
import com.neueda.tms.service.common.DataInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MonitoringRuleRepository ruleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(userRepository, ruleRepository, passwordEncoder);
    }

    @Test
    void firstStartupCreatesTheDefaultAdminAndSeedsEveryMonitoringRule() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("encoded-admin-password");
        when(ruleRepository.existsByRuleCode(anyString())).thenReturn(false);

        dataInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();
        assertThat(savedAdmin.getUsername()).isEqualTo("admin");
        assertThat(savedAdmin.getPasswordHash()).isEqualTo("encoded-admin-password");
        assertThat(savedAdmin.getRole()).isEqualTo(User.UserRole.ADMIN);
        assertThat(savedAdmin.getIsActive()).isTrue();

        ArgumentCaptor<MonitoringRule> ruleCaptor = ArgumentCaptor.forClass(MonitoringRule.class);
        verify(ruleRepository, times(6)).save(ruleCaptor.capture());
        List<MonitoringRule> savedRules = ruleCaptor.getAllValues();
        assertThat(savedRules)
                .extracting(MonitoringRule::getRuleCode)
                .containsExactly(
                        "HIGH_AMOUNT",
                        "RAPID_TRANSACTIONS",
                        "RESTRICTED_COUNTRY",
                        "NEW_CUSTOMER_HIGH_AMOUNT",
                        "ROUND_AMOUNT",
                        "ODD_HOURS"
                );
        assertThat(savedRules).allMatch(rule -> Boolean.TRUE.equals(rule.getIsActive()));
    }

    @Test
    void startupSkipsCreatingTheAdminWhenThatAccountAlreadyExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(ruleRepository.existsByRuleCode(anyString())).thenReturn(false);

        dataInitializer.run();

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(ruleRepository, times(6)).save(any(MonitoringRule.class));
    }

    @Test
    void startupDoesNotDuplicateRulesThatAreAlreadyPresent() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(ruleRepository.existsByRuleCode(anyString())).thenReturn(true);

        dataInitializer.run();

        verify(ruleRepository, never()).save(any(MonitoringRule.class));
    }

    @Test
    void startupOnlyAddsTheRulesThatAreStillMissing() {
        Set<String> existingRules = Set.of("HIGH_AMOUNT", "ROUND_AMOUNT");
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(ruleRepository.existsByRuleCode(anyString()))
                .thenAnswer(invocation -> existingRules.contains((String) invocation.getArgument(0)));

        dataInitializer.run();

        ArgumentCaptor<MonitoringRule> ruleCaptor = ArgumentCaptor.forClass(MonitoringRule.class);
        verify(ruleRepository, times(4)).save(ruleCaptor.capture());
        assertThat(ruleCaptor.getAllValues())
                .extracting(MonitoringRule::getRuleCode)
                .containsExactly(
                        "RAPID_TRANSACTIONS",
                        "RESTRICTED_COUNTRY",
                        "NEW_CUSTOMER_HIGH_AMOUNT",
                        "ODD_HOURS"
                );
    }
}

