package com.github.alphardpaarthurnax.bohcalculator.domain;

import java.util.Objects;

public record Requirement(RequirementKind kind, String targetId, int minimum) {
    public Requirement {
        Objects.requireNonNull(kind, "kind");
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("targetId must not be blank");
        }
        if (minimum < 1) {
            throw new IllegalArgumentException("minimum must be at least 1");
        }
    }
}
