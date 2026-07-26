package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.BoHCalculator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainController {
    @FXML private TabPane mainTabs;
    @FXML private TabPane dataSourceTabs;
    @FXML private Tab dataSourceTab;
    @FXML private Tab elementTab;
    @FXML private Tab recipeTab;
    @FXML private Tab verbTab;
    @FXML private Tab aspectTab;
    @FXML private Tab cardTab;
    @FXML private Tab craftTab;
    @FXML private Tab workstationTab;
    @FXML private Tab otherRecipeTab;
    @FXML private Tab otherVerbTab;
    @FXML private Tab generatorTab;

    private final Map<Tab, String> resources = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        resources.put(elementTab, "element-browser.fxml");
        resources.put(recipeTab, "recipe-browser.fxml");
        resources.put(verbTab, "verb-browser.fxml");
        resources.put(aspectTab, "aspect-browser.fxml");
        resources.put(cardTab, "card-browser.fxml");
        resources.put(craftTab, "craft-browser.fxml");
        resources.put(workstationTab, "workstation-browser.fxml");
        resources.put(otherRecipeTab, "other-recipe-browser.fxml");
        resources.put(otherVerbTab, "other-verb-browser.fxml");
        resources.put(generatorTab, "data-generator.fxml");

        mainTabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, newTab) -> {
                    if (newTab == dataSourceTab) {
                        loadIfNeeded(dataSourceTabs.getSelectionModel().getSelectedItem());
                    } else {
                        loadIfNeeded(newTab);
                    }
                });
        dataSourceTabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, newTab) -> loadIfNeeded(newTab));
        loadIfNeeded(dataSourceTabs.getSelectionModel().getSelectedItem());
    }

    private void loadIfNeeded(Tab tab) {
        if (tab == null || tab.getContent() != null) {
            return;
        }
        String resource = resources.get(tab);
        if (resource == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(BoHCalculator.class.getResource(resource));
            tab.setContent(loader.load());
        } catch (IOException exception) {
            tab.setContent(new Label("页面加载失败：" + exception.getMessage()));
        }
    }
}
