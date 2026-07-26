package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.service.OtherRecipeDataService;
import javafx.collections.ObservableList;

public class OtherRecipeBrowserController extends RecipeBrowserController {
    @Override protected String title() { return "OtherRecipes：Crafts 以外的 Recipe（无图片）"; }
    @Override protected ObservableList<Recipe> sourceItems() { return OtherRecipeDataService.getInstance().getOtherRecipes(); }
}
