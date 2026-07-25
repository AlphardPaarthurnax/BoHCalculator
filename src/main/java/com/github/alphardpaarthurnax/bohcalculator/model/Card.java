package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.scene.image.Image;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {
    private String id;
    private String label;
    private String desc;

    @JsonProperty("imagePath")
    private String imagePath;

    @JsonProperty("sourceFile")
    private String sourceFile;

    @JsonProperty("aspects")
    private Map<String, Integer> aspects = new LinkedHashMap<>();

    @JsonIgnore
    private Image image;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("label")
    @JsonAlias("Label")
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("desc")
    public String getDesc() {
        return desc;
    }

    @JsonProperty("desc")
    @JsonAlias("Desc")
    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Map<String, Integer> getAspects() {
        return aspects;
    }

    public void setAspects(Map<String, Integer> aspects) {
        this.aspects = aspects;
    }

    @JsonIgnore
    public Image getImage() {
        return image;
    }

    @JsonIgnore
    public void setImage(Image image) {
        this.image = image;
    }

    @JsonIgnore
    public String getDisplayName() {
        if (label != null && !label.isEmpty()) {
            return label;
        }
        return id != null ? id : "";
    }
}
