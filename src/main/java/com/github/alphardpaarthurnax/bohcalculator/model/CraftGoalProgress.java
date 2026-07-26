package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.Map;

public record CraftGoalProgress(
        CalculationGoal goal,
        int achieved,
        Map<String, Integer> sources
) {
    public CraftGoalProgress {
        sources = sources == null ? Map.of() : Map.copyOf(sources);
    }
}
