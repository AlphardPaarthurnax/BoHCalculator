package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.Map;

/** Canonical input and expected outcome for an explainable rule scenario. */
public record RuleScenario(
        RecipeDefinition recipe,
        WorkstationDefinition workstation,
        InventorySnapshot inventory,
        Map<Integer, String> placements,
        int expectedEdge,
        String expectedOutput
) {
    public RuleScenario {
        placements = Map.copyOf(placements);
    }
}
