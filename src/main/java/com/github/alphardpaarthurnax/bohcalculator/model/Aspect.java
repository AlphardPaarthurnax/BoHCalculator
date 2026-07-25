package com.github.alphardpaarthurnax.bohcalculator.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.scene.image.Image;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Aspect {
    private String id;
    private String label;
    private String desc;

    @JsonIgnore
    private Image image;

    @JsonIgnore
    private String sourceFile;

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

    @JsonIgnore
    public Image getImage() {
        return image;
    }

    @JsonIgnore
    public void setImage(Image image) {
        this.image = image;
    }

    @JsonIgnore
    public String getSourceFile() {
        return sourceFile;
    }

    @JsonIgnore
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    @JsonIgnore
    public String getDisplayName() {
        if (label != null && !label.isEmpty()) {
            return label;
        }
        return id != null ? id : "";
    }
}
