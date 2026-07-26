package com.github.alphardpaarthurnax.bohcalculator.craft;

import com.github.alphardpaarthurnax.bohcalculator.model.CraftValidationResult;
import com.github.alphardpaarthurnax.bohcalculator.model.ElementDefinition;
import com.github.alphardpaarthurnax.bohcalculator.model.InventorySnapshot;
import com.github.alphardpaarthurnax.bohcalculator.model.RecipeDefinition;
import com.github.alphardpaarthurnax.bohcalculator.model.Requirement;
import com.github.alphardpaarthurnax.bohcalculator.model.RuleViolation;
import com.github.alphardpaarthurnax.bohcalculator.model.ViolationType;
import com.github.alphardpaarthurnax.bohcalculator.model.WorkstationDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validates one explicit card-to-slot assignment against inventory and recipe rules. */
public final class CraftingRuleEvaluator {

    public CraftValidationResult evaluate(
            RecipeDefinition recipe,
            WorkstationDefinition workstation,
            InventorySnapshot inventory,
            Map<Integer, String> placements
    ) {
        List<RuleViolation> violations = new ArrayList<>();
        Map<String, Integer> placedCounts = new HashMap<>();
        Map<String, Integer> aspectTotals = new LinkedHashMap<>();

        for (Map.Entry<Integer, String> placement : placements.entrySet()) {
            int slotNumber = placement.getKey();
            String cardId = placement.getValue();
            if (slotNumber < 1 || slotNumber > workstation.slots().size()) {
                violations.add(new RuleViolation(
                        ViolationType.INVALID_SLOT, cardId, 1, 0, slotNumber));
                continue;
            }

            ElementDefinition card = inventory.definition(cardId);
            if (card == null || inventory.quantity(cardId) == 0) {
                violations.add(new RuleViolation(
                        ViolationType.CARD_NOT_IN_INVENTORY, cardId, 1, 0, slotNumber));
                continue;
            }

            placedCounts.merge(cardId, 1, Integer::sum);
            if (!workstation.slots().get(slotNumber - 1).accepts(card)) {
                violations.add(new RuleViolation(
                        ViolationType.SLOT_REJECTS_CARD, cardId, 1, 0, slotNumber));
            }
            card.aspects().forEach((aspectId, amount) ->
                    aspectTotals.merge(aspectId, amount, Integer::sum));
        }

        placedCounts.forEach((cardId, placed) -> {
            int available = inventory.quantity(cardId);
            if (placed > available) {
                violations.add(new RuleViolation(
                        ViolationType.INVENTORY_QUANTITY_EXCEEDED,
                        cardId,
                        placed,
                        available,
                        0));
            }
        });

        for (Requirement requirement : recipe.requirements()) {
            int actual = switch (requirement.kind()) {
                case ASPECT -> aspectTotals.getOrDefault(requirement.targetId(), 0);
                case CARD -> placedCounts.getOrDefault(requirement.targetId(), 0);
            };
            if (actual < requirement.minimum()) {
                violations.add(new RuleViolation(
                        ViolationType.REQUIREMENT_UNSATISFIED,
                        requirement.targetId(),
                        requirement.minimum(),
                        actual,
                        0));
            }
        }

        return new CraftValidationResult(violations, aspectTotals);
    }
}
