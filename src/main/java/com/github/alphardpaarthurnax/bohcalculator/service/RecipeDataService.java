package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import javafx.collections.ObservableList;

public final class RecipeDataService extends SdeDataService<Recipe> {
    private static final RecipeDataService INSTANCE = new RecipeDataService();

    private RecipeDataService() {
        super("recipes.json", "recipes", Recipe.class, ignored -> true);
    }

    public static RecipeDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Recipe> getRecipes() {
        return getItems();
    }
}
