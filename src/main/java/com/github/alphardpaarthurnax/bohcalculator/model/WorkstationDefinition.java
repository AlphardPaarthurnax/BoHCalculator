package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.List;

public record WorkstationDefinition(String id, List<SlotRule> slots) {
    public WorkstationDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        slots = slots == null ? List.of() : List.copyOf(slots);
    }
}
