package com.github.alphardpaarthurnax.bohcalculator.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecipeDefinition(
        String id,
        List<Requirement> requirements,
        Map<String, Integer> effects
) {
    public RecipeDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        effects = effects == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(effects));
    }
}
