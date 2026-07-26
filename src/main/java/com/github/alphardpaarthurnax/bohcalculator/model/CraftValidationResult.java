package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.Map;
import java.util.List;

public record CraftValidationResult(
        List<RuleViolation> violations,
        Map<String, Integer> aspectTotals
) {
    public CraftValidationResult {
        violations = List.copyOf(violations);
        aspectTotals = Map.copyOf(aspectTotals);
    }

    public boolean isValid() {
        return violations.isEmpty();
    }
}
