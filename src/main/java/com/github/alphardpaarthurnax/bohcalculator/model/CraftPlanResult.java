package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.List;

public record CraftPlanResult(
        boolean complete,
        List<CraftGoalProgress> goals,
        List<CraftPlanStep> steps,
        List<CraftPlanMissing> missing,
        List<String> warnings
) {
    public CraftPlanResult {
        goals = goals == null ? List.of() : List.copyOf(goals);
        steps = steps == null ? List.of() : List.copyOf(steps);
        missing = missing == null ? List.of() : List.copyOf(missing);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
