package com.neueda.tms.service.rule;

import com.neueda.tms.controller.rule.RuleDTO;

import java.util.List;

public interface IRuleService {

    List<RuleDTO.Response> getAllRules();

    RuleDTO.Response getRule(Long id);

    RuleDTO.Response updateRule(Long id, RuleDTO.UpdateRequest request);
}
