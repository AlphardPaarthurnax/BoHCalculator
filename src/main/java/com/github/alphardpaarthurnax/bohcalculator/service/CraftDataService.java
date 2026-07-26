package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.SdeDataService;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import javafx.collections.ObservableList;

public final class CraftDataService extends SdeDataService<Recipe> {
    private static final CraftDataService INSTANCE = new CraftDataService();

    private CraftDataService() {
        super("crafts.json", "crafts", Recipe.class, Recipe::isCraftable);
    }

    public static CraftDataService getInstance() { return INSTANCE; }
    public ObservableList<Recipe> getCrafts() { return getItems(); }
}
