package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import javafx.collections.ObservableList;

public final class OtherRecipeDataService extends SdeDataService<Recipe> {
    private static final OtherRecipeDataService INSTANCE = new OtherRecipeDataService();

    private OtherRecipeDataService() {
        super("other-recipes.json", "otherRecipes", Recipe.class, ignored -> true);
    }

    public static OtherRecipeDataService getInstance() { return INSTANCE; }
    public ObservableList<Recipe> getOtherRecipes() { return getItems(); }
}
