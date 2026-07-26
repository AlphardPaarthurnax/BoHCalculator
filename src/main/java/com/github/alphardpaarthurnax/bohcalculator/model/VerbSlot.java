package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VerbSlot {
    private String label;
    private String description;
    private List<String> essential = new ArrayList<>();
    private List<String> required = new ArrayList<>();
    private List<String> forbidden = new ArrayList<>();
    private boolean greedy;
    private boolean consumes;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getEssential() {
        return essential;
    }

    public void setEssential(List<String> essential) {
        this.essential = essential != null ? essential : new ArrayList<>();
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required != null ? required : new ArrayList<>();
    }

    public List<String> getForbidden() {
        return forbidden;
    }

    public void setForbidden(List<String> forbidden) {
        this.forbidden = forbidden != null ? forbidden : new ArrayList<>();
    }

    public boolean isGreedy() {
        return greedy;
    }

    public void setGreedy(boolean greedy) {
        this.greedy = greedy;
    }

    public boolean isConsumes() {
        return consumes;
    }

    public void setConsumes(boolean consumes) {
        this.consumes = consumes;
    }
}
