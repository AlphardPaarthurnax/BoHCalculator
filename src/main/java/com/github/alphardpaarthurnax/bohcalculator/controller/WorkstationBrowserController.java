package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.VerbSlot;
import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;
import com.github.alphardpaarthurnax.bohcalculator.service.WorkstationDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogBrowserSupport;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogReferenceIndex;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogReferenceViews;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public final class WorkstationBrowserController extends CatalogBrowserSupport<Workstation> {
    @FXML private VBox slotContainer;

    private final CatalogReferenceIndex references = CatalogReferenceIndex.getInstance();

    @Override protected String title() { return "Workstation 浏览器（从可合成 Recipe 与 Verb 槽位关系派生）"; }

    @Override protected ObservableList<Workstation> sourceItems() {
        return WorkstationDataService.getInstance().getWorkstations();
    }

    @Override protected List<DetailRow> details(Workstation item) {
        return List.of(
                row("Aspects", formatMap(item.getAspects())),
                row("可执行配方数", item.getRecipeIds().size()),
                row("槽位数", item.getSlots().size())
        );
    }

    @Override
    protected void onItemShown(Workstation item) {
        slotContainer.getChildren().clear();
        if (item == null) {
            return;
        }
        for (VerbSlot slot : item.getSlots()) {
            Label title = new Label(slot.getLabel() != null && !slot.getLabel().isBlank()
                    ? slot.getLabel() : "未命名槽位");
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

            VBox essential = CatalogReferenceViews.slotSection("Essential", slot.getEssential(), references.aspects());
            VBox required = CatalogReferenceViews.slotSection("Required", slot.getRequired(), references.aspects());
            VBox forbidden = CatalogReferenceViews.slotSection("Forbidden", slot.getForbidden(), references.aspects());
            HBox.setHgrow(essential, Priority.ALWAYS);
            HBox.setHgrow(required, Priority.ALWAYS);
            HBox.setHgrow(forbidden, Priority.ALWAYS);
            essential.setMaxWidth(Double.MAX_VALUE);
            required.setMaxWidth(Double.MAX_VALUE);
            forbidden.setMaxWidth(Double.MAX_VALUE);

            HBox references = new HBox(18, essential, required, forbidden);
            references.setAlignment(Pos.TOP_LEFT);
            VBox slotView = new VBox(7, title, references);
            slotView.setStyle("-fx-padding: 10; -fx-border-color: #d0d0d0; -fx-border-radius: 4;");
            slotContainer.getChildren().add(slotView);
        }
    }

    @Override
    protected boolean includeRawField(Workstation item, String name) {
        return !name.equalsIgnoreCase("Slots");
    }
}
