package com.github.alphardpaarthurnax.bohcalculator.utils;

import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class CatalogBrowserSupport<T extends CatalogItem> {
    @FXML protected Label browserTitle;
    @FXML protected TextField searchField;
    @FXML protected ListView<T> itemList;
    @FXML protected ImageView itemImage;
    @FXML protected Label itemName;
    @FXML protected Label itemId;
    @FXML protected Label itemSource;
    @FXML protected TextFlow itemDescription;
    @FXML protected TableView<DetailRow> detailTable;

    private FilteredList<T> filteredItems;

    protected abstract String title();

    protected abstract ObservableList<T> sourceItems();

    protected abstract List<DetailRow> details(T item);

    protected void onItemShown(T item) {
    }

    protected boolean includeRawField(T item, String name) {
        return true;
    }

    protected boolean displayImages() {
        return true;
    }

    @FXML
    public void initialize() {
        browserTitle.setText(title());
        itemImage.setVisible(displayImages());
        itemImage.setManaged(displayImages());
        setupDetailsTable();

        Collator collator = Collator.getInstance(Locale.CHINA);
        Comparator<T> comparator = (left, right) -> {
            boolean leftLabelled = left.getLabel() != null && !left.getLabel().isBlank();
            boolean rightLabelled = right.getLabel() != null && !right.getLabel().isBlank();
            if (leftLabelled != rightLabelled) {
                return leftLabelled ? -1 : 1;
            }
            return collator.compare(left.getDisplayName(), right.getDisplayName());
        };

        filteredItems = new FilteredList<>(sourceItems());
        SortedList<T> sorted = new SortedList<>(filteredItems, comparator);
        itemList.setItems(sorted);
        itemList.setCellFactory(ignored -> new CatalogListCell<>(displayImages()));

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter(newValue));
        itemList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showItem(newValue));

        if (!sorted.isEmpty()) {
            itemList.getSelectionModel().selectFirst();
        }
    }

    protected DetailRow row(String name, Object value) {
        return new DetailRow(name, value != null ? String.valueOf(value) : "");
    }

    protected String formatMap(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return "—";
        }
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + " × " + entry.getValue())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("—");
    }

    protected String imageFileName(T item) {
        String source = item != null ? item.getRowenariumImageSrc() : null;
        if (source == null || source.isBlank()) {
            return "网页无图片";
        }
        int slash = source.lastIndexOf('/');
        return slash >= 0 ? source.substring(slash + 1) : source;
    }

    private void applyFilter(String text) {
        String needle = text != null ? text.trim().toLowerCase(Locale.ROOT) : "";
        filteredItems.setPredicate(item -> {
            if (needle.isEmpty()) {
                return true;
            }
            return contains(item.getId(), needle)
                    || contains(item.getLabel(), needle)
                    || contains(item.getDesc(), needle)
                    || item.getFields().entrySet().stream()
                    .anyMatch(entry -> contains(entry.getKey(), needle) || contains(entry.getValue(), needle));
        });
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void showItem(T item) {
        if (item == null) {
            itemImage.setImage(null);
            itemName.setText("");
            itemId.setText("");
            itemSource.setText("");
            itemDescription.getChildren().clear();
            detailTable.getItems().clear();
            onItemShown(null);
            return;
        }
        itemImage.setImage(displayImages() ? CatalogImageService.imageFor(item) : null);
        itemName.setText(item.getDisplayName());
        itemId.setText(item.getId());
        itemSource.setText(item.getSourceFile() != null ? "来源：" + item.getSourceFile() : "");
        itemDescription.getChildren().setAll(new Text(item.getDesc() != null ? item.getDesc() : ""));
        List<DetailRow> rows = new ArrayList<>(details(item));
        item.getFields().forEach((name, value) -> {
            boolean alreadyPresent = rows.stream().anyMatch(row -> row.name().equalsIgnoreCase(name));
            if (!alreadyPresent && includeRawField(item, name)
                    && !name.equalsIgnoreCase("Label") && !name.equalsIgnoreCase("Description")) {
                rows.add(row(name, value));
            }
        });
        detailTable.getItems().setAll(rows);
        onItemShown(item);
    }

    @SuppressWarnings("unchecked")
    private void setupDetailsTable() {
        TableColumn<DetailRow, String> name = new TableColumn<>("字段");
        name.setCellValueFactory(value -> new SimpleStringProperty(value.getValue().name()));
        name.setPrefWidth(170);
        TableColumn<DetailRow, String> value = new TableColumn<>("内容");
        value.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().value()));
        value.setPrefWidth(430);
        detailTable.getColumns().setAll(name, value);
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public record DetailRow(String name, String value) {
    }
}
