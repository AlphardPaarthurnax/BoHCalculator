package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Element extends CatalogItem {
    private Map<String, Integer> aspects = new LinkedHashMap<>();
    private boolean aspect;
    private boolean hidden;
    private boolean noArtNeeded;
    private boolean unique;
    private boolean normalImage;
    private String manifestationType;

    public Map<String, Integer> getAspects() {
        return aspects;
    }

    public void setAspects(Map<String, Integer> aspects) {
        this.aspects = aspects != null ? aspects : new LinkedHashMap<>();
    }

    public boolean isAspect() {
        return aspect;
    }

    public void setAspect(boolean aspect) {
        this.aspect = aspect;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isNoArtNeeded() {
        return noArtNeeded;
    }

    public void setNoArtNeeded(boolean noArtNeeded) {
        this.noArtNeeded = noArtNeeded;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isNormalImage() {
        return normalImage;
    }

    public void setNormalImage(boolean normalImage) {
        this.normalImage = normalImage;
    }

    public String getManifestationType() {
        return manifestationType;
    }

    public void setManifestationType(String manifestationType) {
        this.manifestationType = manifestationType;
    }
}
