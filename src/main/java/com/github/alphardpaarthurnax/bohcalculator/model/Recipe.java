package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipe extends CatalogItem {
    private String startDescription;
    private List<String> verbIds = new ArrayList<>();
    private Map<String, Integer> requirements = new LinkedHashMap<>();
    private Map<String, Integer> tableRequirements = new LinkedHashMap<>();
    private Map<String, Integer> extantRequirements = new LinkedHashMap<>();
    private Map<String, Integer> effects = new LinkedHashMap<>();
    private Map<String, Integer> aspects = new LinkedHashMap<>();
    private boolean craftable;

    public String getStartDescription() {
        return startDescription;
    }

    public void setStartDescription(String startDescription) {
        this.startDescription = startDescription;
    }

    public List<String> getVerbIds() {
        return verbIds;
    }

    public void setVerbIds(List<String> verbIds) {
        this.verbIds = verbIds != null ? verbIds : new ArrayList<>();
    }

    public Map<String, Integer> getRequirements() {
        return requirements;
    }

    public void setRequirements(Map<String, Integer> requirements) {
        this.requirements = requirements != null ? requirements : new LinkedHashMap<>();
    }

    public Map<String, Integer> getTableRequirements() {
        return tableRequirements;
    }

    public void setTableRequirements(Map<String, Integer> tableRequirements) {
        this.tableRequirements = tableRequirements != null ? tableRequirements : new LinkedHashMap<>();
    }

    public Map<String, Integer> getExtantRequirements() {
        return extantRequirements;
    }

    public void setExtantRequirements(Map<String, Integer> extantRequirements) {
        this.extantRequirements = extantRequirements != null ? extantRequirements : new LinkedHashMap<>();
    }

    public Map<String, Integer> getEffects() {
        return effects;
    }

    public void setEffects(Map<String, Integer> effects) {
        this.effects = effects != null ? effects : new LinkedHashMap<>();
    }

    public Map<String, Integer> getAspects() {
        return aspects;
    }

    public void setAspects(Map<String, Integer> aspects) {
        this.aspects = aspects != null ? aspects : new LinkedHashMap<>();
    }

    public boolean isCraftable() {
        return craftable;
    }

    public void setCraftable(boolean craftable) {
        this.craftable = craftable;
    }
}
