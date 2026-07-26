package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.service.CatalogReferenceIndex;
import com.github.alphardpaarthurnax.bohcalculator.service.CraftDataService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Map;

public class CraftBrowserController extends RecipeBrowserController {
    @FXML private TableView<CatalogReferenceViews.Entry> requirementsTable;
    @FXML private TableView<CatalogReferenceViews.Entry> specialRequirementsTable;
    @FXML private TableView<CatalogReferenceViews.Entry> effectsTable;

    private final CatalogReferenceIndex references = CatalogReferenceIndex.getInstance();

    @FXML
    @Override
    public void initialize() {
        CatalogReferenceViews.configure(requirementsTable);
        CatalogReferenceViews.configure(specialRequirementsTable);
        CatalogReferenceViews.configure(effectsTable);
        super.initialize();
    }

    @Override protected String title() {
        return "Crafts：可合成且 ID 以 craft. 或 remove. 开头的 Recipe（无图片）";
    }

    @Override protected ObservableList<Recipe> sourceItems() {
        return CraftDataService.getInstance().getCrafts();
    }

    @Override protected List<DetailRow> details(Recipe item) {
        return commonDetails(item);
    }

    @Override protected void onItemShown(Recipe item) {
        Map<String, Integer> requirements = item != null ? item.getRequirements() : Map.of();
        Map<String, Integer> effects = item != null ? item.getEffects() : Map.of();
        CatalogReferenceViews.setMap(requirementsTable, requirements, references.aspects());
        CatalogReferenceViews.setMap(specialRequirementsTable, requirements, references.cards());
        CatalogReferenceViews.setMap(effectsTable, effects, references.all());
    }

    @Override protected boolean includeRawField(Recipe item, String name) {
        return !(name.equalsIgnoreCase("Verb") || name.equalsIgnoreCase("Requirements")
                || name.equalsIgnoreCase("Effects") || name.equalsIgnoreCase("Aspects"));
    }
}
