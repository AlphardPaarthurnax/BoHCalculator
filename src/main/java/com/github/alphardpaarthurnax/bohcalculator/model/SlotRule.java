package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.Set;

/** Normalized constraints for one workstation slot. */
public record SlotRule(
        String label,
        Set<String> essentialAspects,
        Set<String> requiredAnyOfAspects,
        Set<String> forbiddenAspects,
        boolean consumes
) {
    public SlotRule {
        label = label == null ? "" : label;
        essentialAspects = immutableSet(essentialAspects);
        requiredAnyOfAspects = immutableSet(requiredAnyOfAspects);
        forbiddenAspects = immutableSet(forbiddenAspects);
    }

    public boolean accepts(ElementDefinition card) {
        boolean hasEveryEssential = essentialAspects.stream().allMatch(card::hasAspect);
        boolean hasOneRequired = requiredAnyOfAspects.isEmpty()
                || requiredAnyOfAspects.stream().anyMatch(card::hasAspect);
        boolean hasForbidden = forbiddenAspects.stream().anyMatch(card::hasAspect);
        return hasEveryEssential && hasOneRequired && !hasForbidden;
    }

    private static Set<String> immutableSet(Set<String> source) {
        return source == null ? Set.of() : Set.copyOf(source);
    }
}
