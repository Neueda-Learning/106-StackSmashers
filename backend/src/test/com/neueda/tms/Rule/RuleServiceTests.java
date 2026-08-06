package com.neueda.tms.Rule;

import com.neueda.tms.controller.rule.RuleDTO;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.rule.MonitoringRuleRepository;
import com.neueda.tms.service.rule.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceTests {

    @Mock
    private MonitoringRuleRepository ruleRepository;

    private RuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleService = new RuleService(ruleRepository);
    }

    @Test
    void allRulesAreReturnedWithTheirCodesAndActivationState() {
        when(ruleRepository.findAll()).thenReturn(List.of(
                aRule(1L, "HIGH_AMOUNT", true),
                aRule(2L, "ODD_HOURS", false)));

        List<RuleDTO.Response> rules = ruleService.getAllRules();

        assertThat(rules).hasSize(2);
        assertThat(rules).extracting(RuleDTO.Response::getRuleCode)
                .containsExactly("HIGH_AMOUNT", "ODD_HOURS");
        assertThat(rules.get(0).getIsActive()).isTrue();
        assertThat(rules.get(1).getIsActive()).isFalse();
    }

    @Test
    void getAllRulesReturnsEmptyListWhenNoneHaveBeenSeededYet() {
        when(ruleRepository.findAll()).thenReturn(List.of());

        assertThat(ruleService.getAllRules()).isEmpty();
    }

    @Test
    void fetchingAKnownRuleByIdReturnsItsFullDetails() {
        when(ruleRepository.findById(3L)).thenReturn(Optional.of(aRule(3L, "RAPID_TRANSACTIONS", true)));

        RuleDTO.Response response = ruleService.getRule(3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getRuleCode()).isEqualTo("RAPID_TRANSACTIONS");
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getSeverity()).isEqualTo(MonitoringRule.RuleSeverity.HIGH);
    }

    @Test
    void fetchingAnUnknownRuleThrowsAClearNotFoundError() {
        when(ruleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.getRule(404L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Rule not found: 404");
    }

    @Test
    void updatingActiveFlagToFalseDisablesTheRule() {
        MonitoringRule rule = aRule(5L, "HIGH_AMOUNT", true);
        when(ruleRepository.findById(5L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuleDTO.Response response = ruleService.updateRule(5L, new RuleDTO.UpdateRequest(false, null));

        assertThat(response.getIsActive()).isFalse();
        assertThat(response.getRuleCode()).isEqualTo("HIGH_AMOUNT");
    }

    @Test
    void updatingActiveFlagToTrueReEnablesAPreviouslyDisabledRule() {
        MonitoringRule rule = aRule(6L, "ODD_HOURS", false);
        when(ruleRepository.findById(6L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuleDTO.Response response = ruleService.updateRule(6L, new RuleDTO.UpdateRequest(true, null));

        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void updatingParametersReplacesOldThresholdValuesWithNewOnes() {
        MonitoringRule rule = aRule(7L, "HIGH_AMOUNT", true);
        when(ruleRepository.findById(7L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuleDTO.Response response = ruleService.updateRule(7L,
                new RuleDTO.UpdateRequest(null, Map.of("threshold", 50000)));

        assertThat(response.getParameters()).containsEntry("threshold", 50000);
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void updatingWithNullParametersMapsLeavesExistingParametersUntouched() {
        MonitoringRule rule = aRule(8L, "ROUND_AMOUNT", true);
        when(ruleRepository.findById(8L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuleDTO.Response response = ruleService.updateRule(8L, new RuleDTO.UpdateRequest(null, null));

        assertThat(response.getParameters()).containsEntry("threshold", 10000);
    }

    @Test
    void updatingARuleSetsTheUpdatedAtTimestamp() {
        MonitoringRule rule = aRule(9L, "NEW_CUSTOMER_HIGH_AMOUNT", true);
        when(ruleRepository.findById(9L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RuleDTO.Response response = ruleService.updateRule(9L, new RuleDTO.UpdateRequest(false, null));

        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void updatingARuleThatDoesNotExistThrowsAClearError() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.updateRule(999L, new RuleDTO.UpdateRequest(true, null)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Rule not found: 999");
    }

    private MonitoringRule aRule(Long id, String code, boolean active) {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(id);
        rule.setRuleCode(code);
        rule.setRuleName(code + " rule");
        rule.setDescription("Detects " + code.toLowerCase());
        rule.setSeverity(MonitoringRule.RuleSeverity.HIGH);
        rule.setIsActive(active);
        rule.setParameters(Map.of("threshold", 10000));
        rule.setUpdatedAt(LocalDateTime.now());
        return rule;
    }
}
