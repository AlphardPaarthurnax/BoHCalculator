package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.craft.CraftPathPlanner;
import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.CalculationGoal;
import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanResult;
import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Supplies the calculation page with a live snapshot of SDE and stock data. */
public final class CalculationService {
    private static final CalculationService INSTANCE = new CalculationService();

    private final ElementDataService elementData = ElementDataService.getInstance();
    private final AspectDataService aspectData = AspectDataService.getInstance();
    private final CardDataService cardData = CardDataService.getInstance();
    private final ThingDataService thingData = ThingDataService.getInstance();
    private final BookDataService bookData = BookDataService.getInstance();
    private final WallartDataService wallartData = WallartDataService.getInstance();
    private final ComfortDataService comfortData = ComfortDataService.getInstance();
    private final CraftDataService craftData = CraftDataService.getInstance();
    private final WorkstationDataService workstationData = WorkstationDataService.getInstance();
    private final CardStockService cardStock = CardStockService.getInstance();
    private final ThingStockService thingStock = ThingStockService.getInstance();
    private final BookStockService bookStock = BookStockService.getInstance();
    private final WallartStockService wallartStock = WallartStockService.getInstance();
    private final ComfortStockService comfortStock = ComfortStockService.getInstance();
    private final WorkstationStockService workstationStock = WorkstationStockService.getInstance();

    private CalculationService() {
    }

    public static CalculationService getInstance() {
        return INSTANCE;
    }

    public ObservableList<com.github.alphardpaarthurnax.bohcalculator.model.Card> getCards() {
        return cardData.getCards();
    }

    public ObservableList<com.github.alphardpaarthurnax.bohcalculator.model.Thing> getThings() {
        return thingData.getThings();
    }

    public ObservableList<Aspect> getAspects() {
        return aspectData.getAspects();
    }

    public CraftPlanResult calculate(List<CalculationGoal> goals) {
        CraftPathPlanner planner = new CraftPathPlanner(
                planningElements(), aspectData.getAspects(),
                craftData.getCrafts(), workstationData.getWorkstations());
        CraftPlanResult result = planner.plan(goals, inventory(), unlockedWorkstations());
        List<String> warnings = new ArrayList<>(result.warnings());
        warnings.add("最优顺序：先选择缺口最少的路径，再比较合成步骤数和被消耗物品数。");
        warnings.add("已解锁的 Books、Wallarts、Comforts 按各 1 份可用物品计算；Cards、Things 使用库存数量。");
        return new CraftPlanResult(result.complete(), result.goals(), result.steps(),
                result.missing(), warnings);
    }

    public CatalogItem find(String id) {
        for (CatalogItem item : allDisplayItems()) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    private Map<String, Integer> inventory() {
        Map<String, Integer> inventory = new LinkedHashMap<>();
        cardStock.getCards().forEach(card -> putPositive(inventory, card.getId(), cardStock.getQuantity(card.getId())));

        thingStock.getThings().forEach(thing -> putPositive(inventory, thing.getId(), thingStock.getQuantity(thing.getId())));

        bookStock.getBooks().forEach(book -> putPositive(inventory, book.getId(), bookStock.isUnlocked(book.getId()) ? 1 : 0));

        wallartStock.getWallarts().forEach(wallart -> putPositive(inventory, wallart.getId(),
                wallartStock.isUnlocked(wallart.getId()) ? 1 : 0));

        comfortStock.getComforts().forEach(comfort -> putPositive(inventory, comfort.getId(),
                comfortStock.isUnlocked(comfort.getId()) ? 1 : 0));
        return inventory;
    }

    private List<Element> planningElements() {
        Map<String, com.github.alphardpaarthurnax.bohcalculator.model.Card> skills = new LinkedHashMap<>();
        cardData.getCards().stream()
                .filter(cardStock::isSkill)
                .forEach(card -> skills.put(card.getId(), card));
        List<Element> result = new ArrayList<>(elementData.getElements().size());
        for (Element element : elementData.getElements()) {
            com.github.alphardpaarthurnax.bohcalculator.model.Card skill = skills.get(element.getId());
            if (skill == null) {
                result.add(element);
                continue;
            }
            Element adjusted = new Element();
            adjusted.setId(element.getId());
            adjusted.setLabel(element.getLabel());
            adjusted.setDesc(element.getDesc());
            adjusted.setImagePath(element.getImagePath());
            adjusted.setHidden(element.isHidden());
            adjusted.setAspect(element.isAspect());
            adjusted.setAspects(cardStock.getEffectiveAspects(skill));
            result.add(adjusted);
        }
        return result;
    }

    private Set<String> unlockedWorkstations() {
        Set<String> result = new LinkedHashSet<>();
        workstationStock.getWorkstations().stream()
                .filter(workstation -> workstationStock.isUnlocked(workstation.getId()))
                .map(Workstation::getId)
                .forEach(result::add);
        return result;
    }

    private List<CatalogItem> allDisplayItems() {
        List<CatalogItem> items = new ArrayList<>();
        items.addAll(aspectData.getAspects());
        items.addAll(cardData.getCards());
        items.addAll(thingData.getThings());
        items.addAll(bookData.getBooks());
        items.addAll(wallartData.getWallarts());
        items.addAll(comfortData.getComforts());
        items.addAll(workstationData.getWorkstations());
        items.addAll(craftData.getCrafts());
        items.addAll(elementData.getElements());
        return items;
    }

    private void putPositive(Map<String, Integer> target, String id, int amount) {
        if (amount > 0) {
            target.put(id, amount);
        }
    }
}
