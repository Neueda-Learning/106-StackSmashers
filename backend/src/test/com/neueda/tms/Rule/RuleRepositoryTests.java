package com.neueda.tms.Rule;

import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.repository.rule.MonitoringRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleRepositoryTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MonitoringRuleRepository ruleRepository;

    @BeforeEach
    void setUp() {
        ruleRepository = new MonitoringRuleRepository(jdbcTemplate);
    }

    @Test
    void findingAllRulesReturnsEveryRuleOrderedById() {
        MonitoringRule r1 = aRule(1L, "HIGH_AMOUNT");
        MonitoringRule r2 = aRule(2L, "ODD_HOURS");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(r1, r2));

        List<MonitoringRule> rules = ruleRepository.findAll();

        assertThat(rules).hasSize(2);
        assertThat(rules).extracting(MonitoringRule::getRuleCode)
                .containsExactly("HIGH_AMOUNT", "ODD_HOURS");
    }

    @Test
    void findingAllRulesReturnsEmptyListWhenNoRulesHaveBeenSeeded() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        assertThat(ruleRepository.findAll()).isEmpty();
    }

    @Test
    void findingARuleByIdReturnsTheMatchingRecord() {
        MonitoringRule rule = aRule(4L, "RAPID_TRANSACTIONS");
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(4L))).thenReturn(rule);

        Optional<MonitoringRule> result = ruleRepository.findById(4L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(4L);
        assertThat(result.get().getRuleCode()).isEqualTo("RAPID_TRANSACTIONS");
    }

    @Test
    void findingAMissingRuleByIdReturnsEmptyInsteadOfThrowing() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(999L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<MonitoringRule> result = ruleRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findingARuleByRuleCodeReturnsTheCorrectRule() {
        MonitoringRule rule = aRule(5L, "RESTRICTED_COUNTRY");
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("RESTRICTED_COUNTRY"))).thenReturn(rule);

        Optional<MonitoringRule> result = ruleRepository.findByRuleCode("RESTRICTED_COUNTRY");

        assertThat(result).isPresent();
        assertThat(result.get().getRuleCode()).isEqualTo("RESTRICTED_COUNTRY");
    }

    @Test
    void findingAnUnknownRuleCodeReturnsEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("UNKNOWN_CODE")))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<MonitoringRule> result = ruleRepository.findByRuleCode("UNKNOWN_CODE");

        assertThat(result).isEmpty();
    }

    @Test
    void findingActiveRulesReturnsOnlyEnabledRules() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(aRule(1L, "HIGH_AMOUNT"), aRule(3L, "ROUND_AMOUNT")));

        List<MonitoringRule> active = ruleRepository.findByIsActiveTrue();

        assertThat(active).hasSize(2);
        assertThat(active).allMatch(MonitoringRule::getIsActive);
    }

    @Test
    void existsByRuleCodeReturnsTrueWhenARuleWithThatCodeExists() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("HIGH_AMOUNT"))).thenReturn(1L);

        assertThat(ruleRepository.existsByRuleCode("HIGH_AMOUNT")).isTrue();
    }

    @Test
    void existsByRuleCodeReturnsFalseForAnUnknownCode() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("GHOST_RULE"))).thenReturn(0L);

        assertThat(ruleRepository.existsByRuleCode("GHOST_RULE")).isFalse();
    }

    @Test
    void savingANewRuleAssignsTheGeneratedId() {
        MonitoringRule newRule = aRule(null, "NEW_RULE");
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("id", 99L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        MonitoringRule saved = ruleRepository.save(newRule);

        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getRuleCode()).isEqualTo("NEW_RULE");
    }

    @Test
    void savingAnExistingRuleUpdatesItInPlace() {
        MonitoringRule existing = aRule(10L, "HIGH_AMOUNT");
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        MonitoringRule saved = ruleRepository.save(existing);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getRuleCode()).isEqualTo("HIGH_AMOUNT");
    }

    private MonitoringRule aRule(Long id, String code) {
        MonitoringRule rule = new MonitoringRule();
        rule.setId(id);
        rule.setRuleCode(code);
        rule.setRuleName(code + " rule");
        rule.setDescription("Detects " + code.toLowerCase());
        rule.setSeverity(MonitoringRule.RuleSeverity.HIGH);
        rule.setIsActive(true);
        rule.setParameters(Map.of("threshold", 10000));
        return rule;
    }
}
