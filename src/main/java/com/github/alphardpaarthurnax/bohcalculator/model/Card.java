package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Card extends CatalogItem {
    @JsonProperty("aspects")
    private Map<String, Integer> aspects = new LinkedHashMap<>();

    public Map<String, Integer> getAspects() {
        return aspects;
    }

    public void setAspects(Map<String, Integer> aspects) {
        this.aspects = aspects;
    }

}
