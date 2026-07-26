package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import com.github.alphardpaarthurnax.bohcalculator.service.CatalogImageService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

final class CatalogListCell<T extends CatalogItem> extends ListCell<T> {
    private final ImageView icon = new ImageView();
    private final Label label = new Label();
    private final HBox content = new HBox(8);
    private final boolean displayImage;

    CatalogListCell() {
        this(true);
    }

    CatalogListCell(boolean displayImage) {
        this.displayImage = displayImage;
        icon.setFitWidth(24);
        icon.setFitHeight(24);
        icon.setPreserveRatio(true);
        content.setAlignment(Pos.CENTER_LEFT);
        if (displayImage) {
            content.getChildren().add(icon);
        }
        content.getChildren().add(label);
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            return;
        }
        if (displayImage) {
            icon.setImage(CatalogImageService.imageFor(item));
        }
        label.setText(item.getDisplayName());
        setGraphic(content);
        setText(null);
    }
}
