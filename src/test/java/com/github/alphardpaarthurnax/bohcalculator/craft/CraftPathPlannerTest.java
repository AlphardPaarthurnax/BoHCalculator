package com.github.alphardpaarthurnax.bohcalculator.craft;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.CalculationGoal;
import com.github.alphardpaarthurnax.bohcalculator.model.CalculationGoalType;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanResult;
import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.model.VerbSlot;
import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftPathPlannerTest {
    @Test
    void expandsIndependentIntermediateChainsBeforeFinalRecipe() {
        CraftPathPlanner planner = planner(true);

        CraftPlanResult result = planner.plan(
                List.of(new CalculationGoal(CalculationGoalType.ELEMENT, "E", 1)),
                Map.of("A", 1, "C", 1), Set.of("desk"));

        assertTrue(result.complete());
        List<String> recipeIds = result.steps().stream().map(step -> step.recipeId()).toList();
        assertEquals(Set.of("make.B", "make.D"), Set.copyOf(recipeIds.subList(0, 2)));
        assertEquals("make.E", recipeIds.getLast());
        assertEquals(1, result.goals().getFirst().achieved());
        assertTrue(result.missing().isEmpty());
    }

    @Test
    void returnsClosestPartialPathAndNamesMissingIntermediate() {
        CraftPathPlanner planner = planner(false);

        CraftPlanResult result = planner.plan(
                List.of(new CalculationGoal(CalculationGoalType.ELEMENT, "E", 1)),
                Map.of("A", 1), Set.of("desk"));

        assertFalse(result.complete());
        assertTrue(result.steps().stream().anyMatch(step -> step.recipeId().equals("make.B") && step.executable()));
        assertTrue(result.missing().stream().anyMatch(missing -> missing.targetId().equals("D")));
        assertTrue(result.steps().stream().anyMatch(step -> step.recipeId().equals("make.E") && !step.executable()));
    }

    @Test
    void canSatisfyAspectGoalByCraftingAContributingElement() {
        CraftPathPlanner planner = planner(true);

        CraftPlanResult result = planner.plan(
                List.of(new CalculationGoal(CalculationGoalType.ASPECT, "edge", 5)),
                Map.of("A", 1), Set.of("desk"));

        assertTrue(result.complete());
        assertEquals("make.B", result.steps().getFirst().recipeId());
        assertEquals(5, result.goals().getFirst().achieved());
        assertEquals(Map.of("B", 1), result.goals().getFirst().sources());
    }

    @Test
    void preservesAOneOffInventoryItemWhenACurrentlyRenewableInputCanBeUsed() {
        Element rare = element("rare", Map.of());
        Element renewable = element("renewable", Map.of());
        Element seed = element("seed", Map.of());
        Element resultElement = element("result", Map.of());

        Recipe spendRare = recipe("make.result.rare", Map.of("rare", 1), Map.of("result", 1));
        Recipe spendRenewable = recipe("make.result.renewable", Map.of("renewable", 1), Map.of("result", 1));
        Recipe replenish = recipe("make.renewable", Map.of("seed", 1), Map.of("renewable", 1));
        List<Recipe> recipes = List.of(spendRare, spendRenewable, replenish);

        Workstation desk = new Workstation();
        desk.setId("desk");
        desk.setRecipeIds(recipes.stream().map(Recipe::getId).toList());
        desk.setSlots(List.of(slot("材料")));
        CraftPathPlanner planner = new CraftPathPlanner(
                List.of(rare, renewable, seed, resultElement), List.of(), recipes, List.of(desk));

        CraftPlanResult result = planner.plan(
                List.of(new CalculationGoal(CalculationGoalType.ELEMENT, "result", 1)),
                Map.of("rare", 1, "renewable", 1, "seed", 1), Set.of("desk"));

        assertTrue(result.complete());
        assertEquals("make.result.renewable", result.steps().getLast().recipeId());
        assertEquals("renewable", result.steps().getLast().placements().getFirst().elementId());
    }

    private CraftPathPlanner planner(boolean includeC) {
        Element a = element("A", Map.of());
        Element b = element("B", Map.of("edge", 5));
        Element c = element("C", Map.of());
        Element d = element("D", Map.of());
        Element e = element("E", Map.of());
        Aspect edge = new Aspect();
        edge.setId("edge");

        Recipe makeB = recipe("make.B", Map.of("A", 1), Map.of("B", 1));
        Recipe makeD = recipe("make.D", Map.of("C", 1), Map.of("D", 1));
        Recipe makeE = recipe("make.E", Map.of("B", 1, "D", 1), Map.of("E", 1));
        List<Recipe> recipes = includeC ? List.of(makeB, makeD, makeE) : List.of(makeB, makeE);

        Workstation desk = new Workstation();
        desk.setId("desk");
        desk.setRecipeIds(recipes.stream().map(Recipe::getId).toList());
        desk.setSlots(List.of(slot("材料 1"), slot("材料 2")));
        return new CraftPathPlanner(List.of(a, b, c, d, e), List.of(edge), recipes, List.of(desk));
    }

    private Element element(String id, Map<String, Integer> aspects) {
        Element element = new Element();
        element.setId(id);
        element.setAspects(new LinkedHashMap<>(aspects));
        return element;
    }

    private Recipe recipe(String id, Map<String, Integer> requirements, Map<String, Integer> effects) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setRequirements(new LinkedHashMap<>(requirements));
        recipe.setEffects(new LinkedHashMap<>(effects));
        recipe.setCraftable(true);
        recipe.setVerbIds(List.of("desk"));
        return recipe;
    }

    private VerbSlot slot(String label) {
        VerbSlot slot = new VerbSlot();
        slot.setLabel(label);
        slot.setConsumes(true);
        return slot;
    }
}
