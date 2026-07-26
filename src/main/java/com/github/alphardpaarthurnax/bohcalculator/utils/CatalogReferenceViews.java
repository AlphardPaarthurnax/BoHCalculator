package com.github.alphardpaarthurnax.bohcalculator.utils;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CatalogReferenceViews {
    private CatalogReferenceViews() {
    }

    @SuppressWarnings("unchecked")
    public static void configure(TableView<Entry> table) {
        TableColumn<Entry, Void> iconColumn = new TableColumn<>("图标");
        iconColumn.setPrefWidth(44);
        iconColumn.setMinWidth(44);
        iconColumn.setMaxWidth(44);
        iconColumn.setCellFactory(ignored -> new TableCell<>() {
            private final ImageView image = icon(24);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                Entry entry = getTableRow() != null ? getTableRow().getItem() : null;
                if (empty || entry == null) {
                    setGraphic(null);
                } else {
                    image.setImage(CatalogImageService.imageFor(entry.reference()));
                    setGraphic(image);
                }
            }
        });

        TableColumn<Entry, String> nameColumn = new TableColumn<>("名称与数量");
        nameColumn.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().displayText()));
        table.getColumns().setAll(iconColumn, nameColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public static void setMap(TableView<Entry> table, Map<String, Integer> values,
                       Map<String, ? extends CatalogItem> references) {
        List<Entry> rows = new ArrayList<>();
        if (values != null) {
            values.forEach((id, amount) -> {
                CatalogItem reference = references.get(id);
                if (reference != null) {
                    rows.add(new Entry(reference, amount != null ? amount : 1));
                }
            });
        }
        table.getItems().setAll(rows);
        table.setVisible(!rows.isEmpty());
        table.setManaged(!rows.isEmpty());
    }

    public static VBox slotSection(String title, Collection<String> ids, Map<String, Aspect> aspects) {
        VBox section = new VBox(5);
        Label heading = new Label(title);
        heading.setStyle("-fx-font-weight: bold;");
        section.getChildren().add(heading);

        Map<String, Integer> counts = new LinkedHashMap<>();
        if (ids != null) {
            ids.forEach(id -> counts.merge(id, 1, Integer::sum));
        }
        counts.forEach((id, amount) -> {
            Aspect aspect = aspects.get(id);
            if (aspect == null) {
                return;
            }
            ImageView image = icon(22);
            image.setImage(CatalogImageService.imageFor(aspect));
            Label text = new Label(displayText(aspect, amount));
            HBox row = new HBox(7, image, text);
            row.setAlignment(Pos.CENTER_LEFT);
            section.getChildren().add(row);
        });
        return section;
    }

    private static ImageView icon(double size) {
        ImageView image = new ImageView();
        image.setFitWidth(size);
        image.setFitHeight(size);
        image.setPreserveRatio(true);
        return image;
    }

    private static String displayText(CatalogItem reference, int amount) {
        return reference.getDisplayName() + (amount == 1 ? "" : " x " + amount);
    }

    public record Entry(CatalogItem reference, int amount) {
        String displayText() {
            return CatalogReferenceViews.displayText(reference, amount);
        }
    }
}
