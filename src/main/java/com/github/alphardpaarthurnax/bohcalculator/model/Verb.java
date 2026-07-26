package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Verb extends CatalogItem {
    private Map<String, Integer> aspects = new LinkedHashMap<>();
    private List<VerbSlot> slots = new ArrayList<>();

    public Map<String, Integer> getAspects() {
        return aspects;
    }

    public void setAspects(Map<String, Integer> aspects) {
        this.aspects = aspects != null ? aspects : new LinkedHashMap<>();
    }

    public List<VerbSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<VerbSlot> slots) {
        this.slots = slots != null ? slots : new ArrayList<>();
    }
}
