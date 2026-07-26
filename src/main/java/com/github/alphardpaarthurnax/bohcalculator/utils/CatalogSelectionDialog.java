package com.github.alphardpaarthurnax.bohcalculator.utils;

import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/** A searchable, file-chooser-style dialog for selecting a catalog item. */
public final class CatalogSelectionDialog {
    private static final double ICON_SIZE = 64;

    private CatalogSelectionDialog() {
    }

    public static <T extends CatalogItem> Optional<T> show(
            Window owner,
            String title,
            ObservableList<T> source,
            Predicate<T> available,
            Comparator<T> comparator) {
        Dialog<T> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title);
        dialog.setHeaderText("选择要加入库存的物品");

        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField search = new TextField();
        search.setPromptText("搜索名称、ID或说明…");

        FilteredList<T> filtered = new FilteredList<>(source, available);
        SortedList<T> sorted = new SortedList<>(filtered, comparator);
        ListView<T> choices = new ListView<>(sorted);
        choices.setCellFactory(ignored -> new CatalogItemCell<>());
        choices.setPrefHeight(500);

        VBox content = new VBox(10, search, choices);
        content.setPadding(new Insets(4));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(620, 650);

        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        choices.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> addButton.setDisable(newValue == null));

        search.textProperty().addListener((observable, oldValue, newValue) -> {
            String needle = normalize(newValue);
            filtered.setPredicate(item -> available.test(item) && matches(item, needle));
            choices.getSelectionModel().clearSelection();
        });
        search.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN && !choices.getItems().isEmpty()) {
                choices.requestFocus();
                choices.getSelectionModel().selectFirst();
            }
        });
        choices.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && choices.getSelectionModel().getSelectedItem() != null) {
                ((Button) addButton).fire();
            }
        });
        choices.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && choices.getSelectionModel().getSelectedItem() != null) {
                ((Button) addButton).fire();
            }
        });

        dialog.setResultConverter(button -> button == addButtonType
                ? choices.getSelectionModel().getSelectedItem()
                : null);
        dialog.setOnShown(event -> search.requestFocus());
        return dialog.showAndWait();
    }

    private static boolean matches(CatalogItem item, String needle) {
        return needle.isEmpty()
                || contains(item.getId(), needle)
                || contains(item.getLabel(), needle)
                || contains(item.getDesc(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String normalize(String text) {
        return text != null ? text.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static final class CatalogItemCell<T extends CatalogItem> extends ListCell<T> {
        private final ImageView image = new ImageView();
        private final Label name = new Label();
        private final Label id = new Label();
        private final VBox labels = new VBox(4, name, id);
        private final HBox content = new HBox(12, image, labels);

        private CatalogItemCell() {
            image.setFitWidth(ICON_SIZE);
            image.setFitHeight(ICON_SIZE);
            image.setPreserveRatio(true);
            name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
            id.setStyle("-fx-text-fill: #666666;");
            content.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(labels, Priority.ALWAYS);
            setPrefHeight(76);
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                image.setImage(null);
                setGraphic(null);
                return;
            }
            image.setImage(CatalogImageService.imageFor(item));
            name.setText(item.getDisplayName());
            id.setText(item.getId());
            setGraphic(content);
        }
    }
}
