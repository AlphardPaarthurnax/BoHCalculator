package com.github.alphardpaarthurnax.bohcalculator.craft;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.CalculationGoal;
import com.github.alphardpaarthurnax.bohcalculator.model.CalculationGoalType;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftGoalProgress;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlacement;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanMissing;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanMissingType;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanResult;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanStep;
import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.model.VerbSlot;
import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Inventory-aware backward planner. Exact element requirements are recursively
 * expanded, while every concrete craft is validated against a real unlocked
 * workstation and a simultaneous slot assignment.
 */
public final class CraftPathPlanner {
    private static final int MAX_DEPTH = 14;
    private static final int MAX_EXPANSIONS = 12_000;
    private static final int MAX_PLACEMENT_CANDIDATES = 20;

    private final Map<String, Element> elements = new LinkedHashMap<>();
    private final Set<String> aspectIds = new HashSet<>();
    private final List<Recipe> recipes;
    private final Map<String, List<Recipe>> producers = new HashMap<>();
    private final Map<String, Workstation> workstations = new LinkedHashMap<>();
    private final Map<String, List<String>> aspectContributors = new HashMap<>();
    private int expansions;

    public CraftPathPlanner(
            Collection<Element> elements,
            Collection<Aspect> aspects,
            Collection<Recipe> recipes,
            Collection<Workstation> workstations) {
        elements.forEach(element -> this.elements.put(element.getId(), element));
        aspects.forEach(aspect -> aspectIds.add(aspect.getId()));
        this.recipes = recipes.stream().filter(Recipe::isCraftable).toList();
        this.recipes.forEach(recipe -> recipe.getEffects().forEach((id, amount) -> {
            if (amount != null && amount > 0) {
                producers.computeIfAbsent(id, ignored -> new ArrayList<>()).add(recipe);
            }
        }));
        workstations.forEach(workstation -> this.workstations.put(workstation.getId(), workstation));
        this.elements.values().forEach(element -> element.getAspects().forEach((aspectId, amount) -> {
            if (amount != null && amount > 0) {
                aspectContributors.computeIfAbsent(aspectId, ignored -> new ArrayList<>()).add(element.getId());
            }
        }));
    }

    public CraftPlanResult plan(
            List<CalculationGoal> goals,
            Map<String, Integer> initialInventory,
        Set<String> unlockedWorkstations) {
        expansions = 0;
        Map<String, Integer> normalizedInventory = new LinkedHashMap<>(initialInventory);
        normalizedInventory.replaceAll((id, amount) -> memoryLimited(id, amount));
        PlanState state = new PlanState(normalizedInventory);
        for (CalculationGoal goal : goals) {
            Attempt attempt = goal.type() == CalculationGoalType.ELEMENT
                    ? acquireElement(goal.targetId(), goal.amount(), state, unlockedWorkstations,
                    new LinkedHashSet<>(), 0)
                    : acquireAspect(goal.targetId(), goal.amount(), state, unlockedWorkstations,
                    new LinkedHashSet<>(), 0);
            state = attempt.state;
        }

        PlanState finalState = state;
        List<CraftGoalProgress> progress = goals.stream()
                .map(goal -> progress(goal, finalState))
                .toList();
        List<CraftPlanMissing> missing = normalizeMissing(state.missing);
        boolean complete = missing.isEmpty() && progress.stream()
                .allMatch(item -> item.achieved() >= item.goal().amount());
        return new CraftPlanResult(complete, progress, state.steps, missing, state.warnings);
    }

    private Attempt acquireElement(
            String elementId,
            int targetAmount,
            PlanState state,
            Set<String> unlocked,
            LinkedHashSet<String> active,
            int depth) {
        if (state.quantity(elementId) >= targetAmount) {
            return new Attempt(state, true);
        }
        if (!enter("element:" + elementId, active, depth, state)) {
            PlanState limited = state.copy();
            limited.addMissing(CraftPlanMissingType.SEARCH_LIMIT, elementId,
                    targetAmount, state.quantity(elementId), "路径过深或出现循环依赖");
            return new Attempt(limited, false);
        }

        List<Recipe> candidates = producers.getOrDefault(elementId, List.of());
        if (candidates.isEmpty()) {
            PlanState unavailable = state.copy();
            unavailable.addMissing(CraftPlanMissingType.ELEMENT, elementId,
                    targetAmount, state.quantity(elementId), "库存不足，且没有可执行配方产出此物品");
            active.remove("element:" + elementId);
            return new Attempt(unavailable, false);
        }

        Attempt best = null;
        for (Recipe recipe : candidates) {
            PlanState candidate = state.copy();
            int output = Math.max(1, recipe.getEffects().getOrDefault(elementId, 1));
            int executions = (int) Math.ceil((targetAmount - candidate.quantity(elementId)) / (double) output);
            boolean complete = true;
            for (int index = 0; index < executions; index++) {
                Attempt execution = executeRecipe(recipe, candidate, unlocked, active, depth + 1);
                candidate = execution.state;
                if (!execution.complete) {
                    complete = false;
                    break;
                }
            }
            complete &= candidate.quantity(elementId) >= targetAmount;
            Attempt attempt = new Attempt(candidate, complete);
            best = better(best, attempt);
        }
        active.remove("element:" + elementId);
        if (best != null && !best.complete && best.state.quantity(elementId) < targetAmount) {
            best.state.addMissing(CraftPlanMissingType.ELEMENT, elementId, targetAmount,
                    best.state.quantity(elementId), "配方路径仍无法产出足够数量");
        }
        return best != null ? best : new Attempt(state, false);
    }

    private Attempt acquireAspect(
            String aspectId,
            int targetAmount,
            PlanState state,
            Set<String> unlocked,
            LinkedHashSet<String> active,
            int depth) {
        int current = aspectTotal(state, aspectId);
        if (current >= targetAmount) {
            return new Attempt(state, true);
        }
        if (!enter("aspect:" + aspectId, active, depth, state)) {
            PlanState limited = state.copy();
            limited.addMissing(CraftPlanMissingType.SEARCH_LIMIT, aspectId,
                    targetAmount, current, "路径过深或出现循环依赖");
            return new Attempt(limited, false);
        }

        List<String> contributors = new ArrayList<>(aspectContributors.getOrDefault(aspectId, List.of()));
        if (producers.containsKey(aspectId)) {
            contributors.add(aspectId);
        }
        contributors = contributors.stream().distinct()
                .filter(id -> producers.containsKey(id))
                .sorted(Comparator.comparingInt((String id) -> contribution(id, aspectId)).reversed())
                .toList();

        Attempt best = null;
        for (String contributor : contributors) {
            int perCopy = Math.max(1, contribution(contributor, aspectId));
            int copies = (int) Math.ceil((targetAmount - current) / (double) perCopy);
            int targetCopies = state.quantity(contributor) + copies;
            Attempt attempt = acquireElement(contributor, targetCopies, state.copy(), unlocked, active, depth + 1);
            boolean complete = attempt.complete && aspectTotal(attempt.state, aspectId) >= targetAmount;
            best = better(best, new Attempt(attempt.state, complete));
        }

        if (best == null || !best.complete) {
            PlanState closest = best != null ? best.state : state.copy();
            closest.addMissing(CraftPlanMissingType.ASPECT, aspectId,
                    targetAmount, aspectTotal(closest, aspectId),
                    contributors.isEmpty() ? "库存性相不足，且没有能合成的性相来源" : "没有找到可完成的性相来源路径");
            best = new Attempt(closest, false);
        }
        active.remove("aspect:" + aspectId);
        return best;
    }

    private Attempt executeRecipe(
            Recipe recipe,
            PlanState state,
            Set<String> unlocked,
            LinkedHashSet<String> active,
            int depth) {
        if (++expansions > MAX_EXPANSIONS || depth > MAX_DEPTH) {
            PlanState limited = state.copy();
            limited.addMissing(CraftPlanMissingType.SEARCH_LIMIT, recipe.getId(), 1, 0,
                    "已达到搜索上限");
            return blocked(recipe, null, limited);
        }

        List<Workstation> compatible = workstations.values().stream()
                .filter(workstation -> supports(workstation, recipe))
                .toList();
        List<Workstation> available = compatible.stream()
                .filter(workstation -> unlocked.contains(workstation.getId()))
                .toList();
        if (available.isEmpty()) {
            PlanState blocked = state.copy();
            Workstation suggested = compatible.stream().findFirst().orElse(null);
            String workstationId = suggested != null ? suggested.getId() : "workstation";
            blocked.addMissing(CraftPlanMissingType.WORKSTATION, workstationId, 1, 0,
                    suggested == null ? "没有支持此配方的工作台" : "需要先解锁此工作台");
            return blocked(recipe, suggested, blocked);
        }

        Attempt best = null;
        for (Workstation workstation : available) {
            PlanState candidate = state.copy();
            if (workstation.getSlots().stream().anyMatch(VerbSlot::isGreedy)) {
                candidate.addMissing(CraftPlanMissingType.SLOT, workstation.getId(), 1, 0,
                        "该工作台包含尚不支持的 Greedy 槽位");
                best = better(best, blocked(recipe, workstation, candidate));
                continue;
            }
            if (!meetsTableRequirements(recipe, workstation)) {
                candidate.addMissing(CraftPlanMissingType.WORKSTATION, workstation.getId(), 1, 0,
                        "工作台自身性相不满足 Table Requirements");
                best = better(best, blocked(recipe, workstation, candidate));
                continue;
            }

            boolean inputsComplete = true;
            for (Map.Entry<String, Integer> requirement : exactRequirements(recipe).entrySet()) {
                Attempt input = acquireElement(requirement.getKey(), requirement.getValue(), candidate,
                        unlocked, active, depth + 1);
                candidate = input.state;
                inputsComplete &= input.complete;
            }
            if (!inputsComplete) {
                best = better(best, blocked(recipe, workstation, candidate));
                continue;
            }

            PlacementResult placement = findPlacement(recipe, workstation, candidate);
            if (!placement.valid) {
                Attempt supplied = supplyAspectContributors(recipe, workstation, candidate,
                        unlocked, active, depth + 1, placement);
                candidate = supplied.state;
                placement = findPlacement(recipe, workstation, candidate);
            }
            if (!placement.valid || !meetsExtantRequirements(recipe, candidate)) {
                addPlacementMissing(recipe, workstation, candidate, placement);
                best = better(best, blocked(recipe, workstation, candidate, placement.placements));
                continue;
            }

            for (CraftPlacement placed : placement.placements) {
                if (placed.consumed()) {
                    candidate.remove(placed.elementId(), 1);
                    candidate.consumed++;
                }
            }
            for (Map.Entry<String, Integer> effect : recipe.getEffects().entrySet()) {
                String id = effect.getKey();
                Integer amount = effect.getValue();
                if (amount != null && amount > 0) {
                    candidate.add(id, memoryLimited(id, candidate.quantity(id) + amount));
                }
            }
            candidate.steps.add(new CraftPlanStep(recipe.getId(), workstation.getId(),
                    placement.placements, positiveEffects(recipe), true));
            best = better(best, new Attempt(candidate, true));
        }
        if (best != null && !best.complete
                && best.state.missing.stream().anyMatch(item -> item.type() == CraftPlanMissingType.SLOT)) {
            PlanState closestState = best.state;
            compatible.stream()
                    .filter(workstation -> !unlocked.contains(workstation.getId()))
                    .filter(workstation -> workstation.getSlots().stream().noneMatch(VerbSlot::isGreedy))
                    .filter(workstation -> meetsTableRequirements(recipe, workstation))
                    .findFirst()
                    .ifPresent(workstation -> closestState.addMissing(
                            CraftPlanMissingType.WORKSTATION, workstation.getId(), 1, 0,
                            "当前已解锁工作台的槽位无法完成此步骤；这是可尝试解锁的兼容工作台"));
        }
        return best != null ? best : new Attempt(state, false);
    }

    private Attempt supplyAspectContributors(
            Recipe recipe,
            Workstation workstation,
            PlanState state,
            Set<String> unlocked,
            LinkedHashSet<String> active,
            int depth,
            PlacementResult starting) {
        PlanState working = state;
        PlacementResult placement = starting;
        int attempts = 0;
        while (!placement.valid && attempts++ < workstation.getSlots().size()) {
            String deficitAspect = largestAspectDeficit(recipe, placement);
            if (deficitAspect == null) {
                break;
            }
            Attempt best = null;
            int bestCoverage = placement.coverage;
            for (String contributor : aspectContributors.getOrDefault(deficitAspect, List.of()).stream()
                    .filter(producers::containsKey)
                    .filter(id -> acceptedByAnySlot(workstation, elements.get(id)))
                    .sorted(Comparator.comparingInt((String id) -> contribution(id, deficitAspect)).reversed())
                    .limit(12)
                    .toList()) {
                PlanState trial = working.copy();
                Attempt acquired = acquireElement(contributor, trial.quantity(contributor) + 1,
                        trial, unlocked, active, depth + 1);
                if (!acquired.complete) {
                    continue;
                }
                PlacementResult trialPlacement = findPlacement(recipe, workstation, acquired.state);
                if (trialPlacement.valid || trialPlacement.coverage > bestCoverage) {
                    best = acquired;
                    bestCoverage = trialPlacement.coverage;
                    if (trialPlacement.valid) {
                        break;
                    }
                }
            }
            if (best == null) {
                break;
            }
            working = best.state;
            placement = findPlacement(recipe, workstation, working);
        }
        return new Attempt(working, placement.valid);
    }

    private PlacementResult findPlacement(Recipe recipe, Workstation workstation, PlanState state) {
        Map<String, Integer> exact = exactRequirements(recipe);
        Map<String, Integer> aspects = aspectRequirements(recipe);
        List<String> candidates = state.inventory.entrySet().stream()
                .filter(entry -> entry.getValue() > 0 && elements.containsKey(entry.getKey()))
                .map(Map.Entry::getKey)
                .filter(id -> exact.containsKey(id) || aspects.keySet().stream()
                        .anyMatch(aspect -> contribution(id, aspect) > 0))
                .sorted(Comparator.comparingInt((String id) -> placementUtility(id, exact, aspects)).reversed())
                .limit(MAX_PLACEMENT_CANDIDATES)
                .toList();
        PlacementSearch search = new PlacementSearch(recipe, workstation, state, candidates, exact, aspects);
        search.visit(0, new HashMap<>(), new ArrayList<>());
        return search.best != null ? search.best : new PlacementResult(false, List.of(), Map.of(), Map.of(), 0);
    }

    private final class PlacementSearch {
        private final Recipe recipe;
        private final Workstation workstation;
        private final PlanState state;
        private final List<String> candidates;
        private final Map<String, Integer> exact;
        private final Map<String, Integer> aspects;
        private PlacementResult best;
        private boolean found;

        private PlacementSearch(Recipe recipe, Workstation workstation, PlanState state,
                                List<String> candidates, Map<String, Integer> exact,
                                Map<String, Integer> aspects) {
            this.recipe = recipe;
            this.workstation = workstation;
            this.state = state;
            this.candidates = candidates;
            this.exact = exact;
            this.aspects = aspects;
        }

        private void visit(int slotIndex, Map<String, Integer> used, List<CraftPlacement> placed) {
            if (found) {
                return;
            }
            PlacementResult current = evaluate(placed);
            if (best == null || current.coverage > best.coverage) {
                best = current;
            }
            if (current.valid) {
                best = current;
                found = true;
                return;
            }
            if (slotIndex >= workstation.getSlots().size()) {
                return;
            }

            VerbSlot slot = workstation.getSlots().get(slotIndex);
            for (String id : candidates) {
                if (used.getOrDefault(id, 0) >= state.quantity(id)) {
                    continue;
                }
                Element element = elements.get(id);
                if (!accepts(slot, element)) {
                    continue;
                }
                used.merge(id, 1, Integer::sum);
                placed.add(new CraftPlacement(slotLabel(slot, slotIndex), id, slot.isConsumes()));
                visit(slotIndex + 1, used, placed);
                placed.remove(placed.size() - 1);
                used.compute(id, (ignored, count) -> count == null || count <= 1 ? null : count - 1);
            }
            visit(slotIndex + 1, used, placed);
        }

        private PlacementResult evaluate(List<CraftPlacement> placed) {
            Map<String, Integer> counts = new HashMap<>();
            Map<String, Integer> totals = new HashMap<>();
            for (CraftPlacement placement : placed) {
                counts.merge(placement.elementId(), 1, Integer::sum);
                Element element = elements.get(placement.elementId());
                element.getAspects().forEach((id, amount) -> totals.merge(id, amount, Integer::sum));
            }
            boolean valid = exact.entrySet().stream()
                    .allMatch(entry -> counts.getOrDefault(entry.getKey(), 0) >= entry.getValue())
                    && aspects.entrySet().stream()
                    .allMatch(entry -> totals.getOrDefault(entry.getKey(), 0) >= entry.getValue());
            int coverage = 0;
            for (Map.Entry<String, Integer> entry : exact.entrySet()) {
                coverage += 1000 * Math.min(entry.getValue(), counts.getOrDefault(entry.getKey(), 0));
            }
            for (Map.Entry<String, Integer> entry : aspects.entrySet()) {
                coverage += Math.min(entry.getValue(), totals.getOrDefault(entry.getKey(), 0));
            }
            return new PlacementResult(valid, List.copyOf(placed), totals, counts, coverage);
        }
    }

    private Attempt blocked(Recipe recipe, Workstation workstation, PlanState state) {
        return blocked(recipe, workstation, state, List.of());
    }

    private Attempt blocked(Recipe recipe, Workstation workstation, PlanState state,
                            List<CraftPlacement> placements) {
        state.steps.add(new CraftPlanStep(recipe.getId(), workstation != null ? workstation.getId() : null,
                placements, positiveEffects(recipe), false));
        return new Attempt(state, false);
    }

    private void addPlacementMissing(Recipe recipe, Workstation workstation,
                                     PlanState state, PlacementResult placement) {
        for (Map.Entry<String, Integer> requirement : exactRequirements(recipe).entrySet()) {
            int available = placement.exactCounts.getOrDefault(requirement.getKey(), 0);
            if (available < requirement.getValue()) {
                state.addMissing(CraftPlanMissingType.SLOT, requirement.getKey(),
                        requirement.getValue(), available,
                        "库存中已有物品，但无法同时放入“" + workstation.getDisplayName() + "”的槽位");
            }
        }
        for (Map.Entry<String, Integer> requirement : aspectRequirements(recipe).entrySet()) {
            int available = placement.aspectTotals.getOrDefault(requirement.getKey(), 0);
            if (available < requirement.getValue()) {
                state.addMissing(CraftPlanMissingType.ASPECT, requirement.getKey(),
                        requirement.getValue(), available,
                        "工作台槽位内可同时提供的性相不足");
            }
        }
        for (Map.Entry<String, Integer> requirement : recipe.getExtantRequirements().entrySet()) {
            int available = requirementAmount(requirement.getKey(), state);
            if (available < requirement.getValue()) {
                state.addMissing(aspectIds.contains(requirement.getKey())
                                ? CraftPlanMissingType.ASPECT : CraftPlanMissingType.ELEMENT,
                        requirement.getKey(), requirement.getValue(), available,
                        "Extant Requirements 未满足");
            }
        }
    }

    private boolean meetsTableRequirements(Recipe recipe, Workstation workstation) {
        return recipe.getTableRequirements().entrySet().stream().allMatch(requirement ->
                workstation.getAspects().getOrDefault(requirement.getKey(), 0) >= requirement.getValue());
    }

    private boolean meetsExtantRequirements(Recipe recipe, PlanState state) {
        return recipe.getExtantRequirements().entrySet().stream()
                .allMatch(entry -> requirementAmount(entry.getKey(), state) >= entry.getValue());
    }

    private int requirementAmount(String id, PlanState state) {
        return aspectIds.contains(id) ? aspectTotal(state, id) : state.quantity(id);
    }

    private Map<String, Integer> aspectRequirements(Recipe recipe) {
        Map<String, Integer> result = new LinkedHashMap<>();
        recipe.getRequirements().forEach((id, amount) -> {
            if (aspectIds.contains(id) && amount != null && amount > 0) {
                result.put(id, amount);
            }
        });
        return result;
    }

    private Map<String, Integer> exactRequirements(Recipe recipe) {
        Map<String, Integer> result = new LinkedHashMap<>();
        recipe.getRequirements().forEach((id, amount) -> {
            if (!aspectIds.contains(id) && amount != null && amount > 0) {
                result.put(id, amount);
            }
        });
        return result;
    }

    private boolean supports(Workstation workstation, Recipe recipe) {
        return workstation.getRecipeIds().contains(recipe.getId())
                || recipe.getVerbIds().contains(workstation.getId());
    }

    private boolean accepts(VerbSlot slot, Element element) {
        if (element == null) {
            return false;
        }
        boolean essentials = slot.getEssential().stream().allMatch(id -> contribution(element.getId(), id) > 0);
        boolean required = slot.getRequired().isEmpty()
                || slot.getRequired().stream().anyMatch(id -> contribution(element.getId(), id) > 0);
        boolean forbidden = slot.getForbidden().stream().anyMatch(id -> contribution(element.getId(), id) > 0);
        return essentials && required && !forbidden;
    }

    private boolean acceptedByAnySlot(Workstation workstation, Element element) {
        return workstation.getSlots().stream().anyMatch(slot -> accepts(slot, element));
    }

    private int placementUtility(String id, Map<String, Integer> exact, Map<String, Integer> aspects) {
        int utility = exact.containsKey(id) ? 10_000 : 0;
        for (String aspect : aspects.keySet()) {
            utility += contribution(id, aspect);
        }
        return utility;
    }

    private int contribution(String elementId, String aspectId) {
        if (Objects.equals(elementId, aspectId) && aspectIds.contains(aspectId)) {
            return 1;
        }
        Element element = elements.get(elementId);
        return element != null ? Math.max(0, element.getAspects().getOrDefault(aspectId, 0)) : 0;
    }

    private int aspectTotal(PlanState state, String aspectId) {
        int total = aspectIds.contains(aspectId) ? state.quantity(aspectId) : 0;
        for (Map.Entry<String, Integer> entry : state.inventory.entrySet()) {
            if (!Objects.equals(entry.getKey(), aspectId)) {
                total += contribution(entry.getKey(), aspectId) * entry.getValue();
            }
        }
        return total;
    }

    private String largestAspectDeficit(Recipe recipe, PlacementResult placement) {
        return aspectRequirements(recipe).entrySet().stream()
                .filter(entry -> placement.aspectTotals.getOrDefault(entry.getKey(), 0) < entry.getValue())
                .max(Comparator.comparingInt(entry -> entry.getValue()
                        - placement.aspectTotals.getOrDefault(entry.getKey(), 0)))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private CraftGoalProgress progress(CalculationGoal goal, PlanState state) {
        int achieved = goal.type() == CalculationGoalType.ELEMENT
                ? state.quantity(goal.targetId())
                : aspectTotal(state, goal.targetId());
        Map<String, Integer> sources = goal.type() == CalculationGoalType.ASPECT
                ? aspectSources(state, goal.targetId(), goal.amount())
                : Map.of(goal.targetId(), Math.min(goal.amount(), achieved));
        return new CraftGoalProgress(goal, achieved, sources);
    }

    private Map<String, Integer> aspectSources(PlanState state, String aspectId, int target) {
        Map<String, Integer> result = new LinkedHashMap<>();
        int remaining = target;
        List<Map.Entry<String, Integer>> sources = state.inventory.entrySet().stream()
                .filter(entry -> entry.getValue() > 0 && contribution(entry.getKey(), aspectId) > 0)
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> entry) ->
                        contribution(entry.getKey(), aspectId)).reversed())
                .toList();
        for (Map.Entry<String, Integer> source : sources) {
            if (remaining <= 0) {
                break;
            }
            int value = contribution(source.getKey(), aspectId);
            int copies = Math.min(source.getValue(), (int) Math.ceil(remaining / (double) value));
            result.put(source.getKey(), copies);
            remaining -= copies * value;
        }
        if (remaining > 0 && state.quantity(aspectId) > 0) {
            int direct = Math.min(remaining, state.quantity(aspectId));
            result.put(aspectId, direct);
        }
        return result;
    }

    private int memoryLimited(String id, int requested) {
        Element element = elements.get(id);
        return element != null && element.getAspects().getOrDefault("memory", 0) > 0
                ? Math.min(1, requested)
                : requested;
    }

    private Map<String, Integer> positiveEffects(Recipe recipe) {
        Map<String, Integer> effects = new LinkedHashMap<>();
        recipe.getEffects().forEach((id, amount) -> {
            if (amount != null && amount > 0) {
                effects.put(id, amount);
            }
        });
        return effects;
    }

    private boolean enter(String key, Set<String> active, int depth, PlanState state) {
        if (depth > MAX_DEPTH || expansions > MAX_EXPANSIONS || active.contains(key)) {
            return false;
        }
        active.add(key);
        return true;
    }

    private Attempt better(Attempt current, Attempt candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        if (candidate.complete != current.complete) {
            return candidate.complete ? candidate : current;
        }
        int candidateMissing = missingScore(candidate.state.missing);
        int currentMissing = missingScore(current.state.missing);
        if (candidateMissing != currentMissing) {
            return candidateMissing < currentMissing ? candidate : current;
        }
        if (candidate.state.steps.size() != current.state.steps.size()) {
            return candidate.state.steps.size() < current.state.steps.size() ? candidate : current;
        }
        return candidate.state.consumed < current.state.consumed ? candidate : current;
    }

    private int missingScore(List<CraftPlanMissing> missing) {
        int score = 0;
        for (CraftPlanMissing item : normalizeMissing(missing)) {
            int deficit = Math.max(1, item.required() - item.available());
            score += switch (item.type()) {
                case WORKSTATION -> 10_000;
                case SEARCH_LIMIT -> 20_000;
                case SLOT -> 5_000 + deficit;
                case ELEMENT -> 100 + deficit;
                case ASPECT -> deficit;
            };
        }
        return score;
    }

    private List<CraftPlanMissing> normalizeMissing(List<CraftPlanMissing> source) {
        Map<String, CraftPlanMissing> merged = new LinkedHashMap<>();
        for (CraftPlanMissing item : source) {
            String key = item.type() + "|" + item.targetId() + "|" + item.detail();
            merged.merge(key, item, (left, right) -> new CraftPlanMissing(
                    left.type(), left.targetId(), Math.max(left.required(), right.required()),
                    Math.max(left.available(), right.available()), left.detail()));
        }
        return List.copyOf(merged.values());
    }

    private String slotLabel(VerbSlot slot, int index) {
        return slot.getLabel() != null && !slot.getLabel().isBlank()
                ? slot.getLabel() : "槽位 " + (index + 1);
    }

    private record Attempt(PlanState state, boolean complete) {
    }

    private record PlacementResult(
            boolean valid,
            List<CraftPlacement> placements,
            Map<String, Integer> aspectTotals,
            Map<String, Integer> exactCounts,
            int coverage) {
    }

    private static final class PlanState {
        private final Map<String, Integer> inventory;
        private final List<CraftPlanStep> steps;
        private final List<CraftPlanMissing> missing;
        private final List<String> warnings;
        private int consumed;

        private PlanState(Map<String, Integer> inventory) {
            this(new LinkedHashMap<>(inventory), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
            this.inventory.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() <= 0);
        }

        private PlanState(Map<String, Integer> inventory, List<CraftPlanStep> steps,
                          List<CraftPlanMissing> missing, List<String> warnings, int consumed) {
            this.inventory = inventory;
            this.steps = steps;
            this.missing = missing;
            this.warnings = warnings;
            this.consumed = consumed;
        }

        private PlanState copy() {
            return new PlanState(new LinkedHashMap<>(inventory), new ArrayList<>(steps),
                    new ArrayList<>(missing), new ArrayList<>(warnings), consumed);
        }

        private int quantity(String id) {
            return inventory.getOrDefault(id, 0);
        }

        private void add(String id, int resultingQuantity) {
            if (resultingQuantity > 0) {
                inventory.put(id, resultingQuantity);
            }
        }

        private void remove(String id, int amount) {
            int remaining = quantity(id) - amount;
            if (remaining > 0) {
                inventory.put(id, remaining);
            } else {
                inventory.remove(id);
            }
        }

        private void addMissing(CraftPlanMissingType type, String id,
                                int required, int available, String detail) {
            missing.add(new CraftPlanMissing(type, id, required, available, detail));
        }
    }
}
