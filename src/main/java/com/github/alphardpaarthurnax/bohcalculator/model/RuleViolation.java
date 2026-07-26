package com.github.alphardpaarthurnax.bohcalculator.model;

/** slotNumber is one-based; zero denotes a recipe/global violation. */
public record RuleViolation(
        ViolationType type,
        String subjectId,
        int required,
        int actual,
        int slotNumber
) {
}
