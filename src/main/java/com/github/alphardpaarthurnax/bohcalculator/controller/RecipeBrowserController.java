package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.service.RecipeDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.RecipeBrowserSupport;
import javafx.collections.ObservableList;

public final class RecipeBrowserController extends RecipeBrowserSupport {
    @Override
    protected String title() {
        return "Recipes：Rowenarium 抓取的全部 Recipe（无图片）";
    }

    @Override
    protected ObservableList<Recipe> sourceItems() {
        return RecipeDataService.getInstance().getRecipes();
    }
}
