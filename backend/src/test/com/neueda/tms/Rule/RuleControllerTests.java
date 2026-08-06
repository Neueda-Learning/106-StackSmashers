package com.neueda.tms.Rule;

import com.neueda.tms.controller.rule.RuleController;
import com.neueda.tms.controller.rule.RuleDTO;
import com.neueda.tms.repository.rule.MonitoringRule;
import com.neueda.tms.service.rule.IRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleControllerTests {

    @Mock
    private IRuleService ruleService;

    private RuleController ruleController;

    @BeforeEach
    void setUp() {
        ruleController = new RuleController(ruleService);
    }

    @Test
    void listingAllRulesReturnsEveryActiveAndInactiveRuleForTheAnalyst() {
        when(ruleService.getAllRules()).thenReturn(List.of(
                aRuleResponse(1L, "HIGH_AMOUNT", true),
                aRuleResponse(2L, "ODD_HOURS", false)));

        ResponseEntity<List<RuleDTO.Response>> response = ruleController.getAllRules();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).extracting(RuleDTO.Response::getRuleCode)
                .containsExactly("HIGH_AMOUNT", "ODD_HOURS");
    }

    @Test
    void listingRulesReturnsEmptyCollectionWhenNoRulesHaveBeenSeededYet() {
        when(ruleService.getAllRules()).thenReturn(List.of());

        ResponseEntity<List<RuleDTO.Response>> response = ruleController.getAllRules();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void fetchingASingleRuleReturnsItsFullDetails() {
        when(ruleService.getRule(3L)).thenReturn(aRuleResponse(3L, "RAPID_TRANSACTIONS", true));

        ResponseEntity<RuleDTO.Response> response = ruleController.getRule(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(3L);
        assertThat(response.getBody().getRuleCode()).isEqualTo("RAPID_TRANSACTIONS");
        assertThat(response.getBody().getIsActive()).isTrue();
    }

    @Test
    void fetchingARuleThatDoesNotExistBubblesUpTheNotFoundError() {
        when(ruleService.getRule(999L)).thenThrow(new NoSuchElementException("Rule not found: 999"));

        assertThatThrownBy(() -> ruleController.getRule(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Rule not found: 999");
    }

    @Test
    void adminCanDeactivateARuleByUpdatingItsActiveFlag() {
        RuleDTO.UpdateRequest request = new RuleDTO.UpdateRequest(false, null);
        RuleDTO.Response deactivated = aRuleResponse(4L, "HIGH_AMOUNT", false);
        when(ruleService.updateRule(4L, request)).thenReturn(deactivated);

        ResponseEntity<RuleDTO.Response> response = ruleController.updateRule(4L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsActive()).isFalse();
    }

    @Test
    void adminCanChangedRuleParametersWithoutTouchingItsActivationStatus() {
        RuleDTO.UpdateRequest request = new RuleDTO.UpdateRequest(null, Map.of("threshold", 25000));
        RuleDTO.Response updated = aRuleResponse(5L, "HIGH_AMOUNT", true);
        updated.setParameters(Map.of("threshold", 25000));
        when(ruleService.updateRule(5L, request)).thenReturn(updated);

        ResponseEntity<RuleDTO.Response> response = ruleController.updateRule(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getParameters()).containsEntry("threshold", 25000);
        assertThat(response.getBody().getIsActive()).isTrue();
    }

    @Test
    void updatingARuleThatDoesNotExistBubblesUpTheNotFoundError() {
        RuleDTO.UpdateRequest request = new RuleDTO.UpdateRequest(true, null);
        when(ruleService.updateRule(888L, request)).thenThrow(new NoSuchElementException("Rule not found: 888"));

        assertThatThrownBy(() -> ruleController.updateRule(888L, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("888");
    }

    private RuleDTO.Response aRuleResponse(Long id, String code, boolean active) {
        return new RuleDTO.Response(id, code, code + " name", "Rule description",
                MonitoringRule.RuleSeverity.HIGH, active, Map.of("threshold", 10000), LocalDateTime.now());
    }
}
