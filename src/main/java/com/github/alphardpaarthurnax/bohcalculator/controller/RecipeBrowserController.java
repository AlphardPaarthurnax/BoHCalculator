package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.service.RecipeDataService;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class RecipeBrowserController extends CatalogBrowserController<Recipe> {
    @Override protected String title() { return "Recipes：Rowenarium 抓取的全部 Recipe（无图片）"; }

    @Override protected boolean displayImages() { return false; }

    @Override protected ObservableList<Recipe> sourceItems() {
        return RecipeDataService.getInstance().getRecipes();
    }

    @Override protected List<DetailRow> details(Recipe item) {
        List<DetailRow> rows = commonDetails(item);
        rows.add(1, row("Verb", String.join("\n", item.getVerbIds())));
        rows.add(2, row("Requirements", formatMap(item.getRequirements())));
        rows.add(3, row("Effects", formatMap(item.getEffects())));
        rows.add(4, row("Aspects", formatMap(item.getAspects())));
        return rows;
    }

    protected List<DetailRow> commonDetails(Recipe item) {
        List<DetailRow> rows = new ArrayList<>();
        rows.add(row("可合成", item.isCraftable() ? "是" : "否"));
        rows.add(row("Table Requirements", formatMap(item.getTableRequirements())));
        rows.add(row("Extant Requirements", formatMap(item.getExtantRequirements())));
        rows.add(row("开始描述", item.getStartDescription()));
        return rows;
    }
}
