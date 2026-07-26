package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Workstation extends Verb {
    private List<String> recipeIds = new ArrayList<>();

    public List<String> getRecipeIds() {
        return recipeIds;
    }

    public void setRecipeIds(List<String> recipeIds) {
        this.recipeIds = recipeIds != null ? recipeIds : new ArrayList<>();
    }
}
