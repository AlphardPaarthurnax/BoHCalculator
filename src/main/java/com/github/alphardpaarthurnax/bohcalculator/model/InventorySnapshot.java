package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.Map;

/** Immutable inventory view used by rule evaluation and, later, path search. */
public final class InventorySnapshot {
    private final Map<String, ElementDefinition> definitions;
    private final Map<String, Integer> quantities;

    public InventorySnapshot(
            Map<String, ElementDefinition> definitions,
            Map<String, Integer> quantities
    ) {
        this.definitions = Map.copyOf(definitions);
        this.quantities = Map.copyOf(quantities);
        validate();
    }

    public ElementDefinition definition(String cardId) {
        return definitions.get(cardId);
    }

    public int quantity(String cardId) {
        return quantities.getOrDefault(cardId, 0);
    }

    private void validate() {
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            String cardId = entry.getKey();
            Integer quantity = entry.getValue();
            ElementDefinition definition = definitions.get(cardId);
            if (definition == null) {
                throw new IllegalArgumentException("unknown inventory card: " + cardId);
            }
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("quantity must not be negative: " + cardId);
            }
            if (definition.isMemory() && quantity > 1) {
                throw new IllegalArgumentException(
                        "a memory card can have at most one copy: " + cardId);
            }
        }
    }
}
