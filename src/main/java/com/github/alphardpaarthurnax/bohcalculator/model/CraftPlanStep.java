package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.List;
import java.util.Map;

public record CraftPlanStep(
        String recipeId,
        String workstationId,
        List<CraftPlacement> placements,
        Map<String, Integer> effects,
        boolean executable
) {
    public CraftPlanStep {
        placements = placements == null ? List.of() : List.copyOf(placements);
        effects = effects == null ? Map.of() : Map.copyOf(effects);
    }
}
