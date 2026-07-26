package com.github.alphardpaarthurnax.bohcalculator.utils;

import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public abstract class QuantityStockSupport<T extends CatalogItem> {
    private static final int MAX_QUANTITY = 999_999;

    @FXML protected TextField searchField;
    @FXML protected ListView<T> itemList;
    @FXML protected ImageView itemImage;
    @FXML protected Label itemName;
    @FXML protected Label itemId;
    @FXML protected Spinner<Integer> quantitySpinner;
    @FXML protected Label saveStatus;

    private FilteredList<T> filteredItems;
    private Comparator<T> itemComparator;
    private String searchText = "";
    private boolean updatingSpinner;

    protected abstract ObservableList<T> sourceItems();

    protected abstract int quantity(String id);

    protected abstract void setQuantity(String id, int amount);

    @FXML
    public void initialize() {
        quantitySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, MAX_QUANTITY, 0));

        Collator collator = Collator.getInstance(Locale.CHINA);
        itemComparator = (left, right) -> {
            int byName = collator.compare(left.getDisplayName(), right.getDisplayName());
            if (byName != 0) {
                return byName;
            }
            String leftId = left.getId() != null ? left.getId() : "";
            String rightId = right.getId() != null ? right.getId() : "";
            return leftId.compareTo(rightId);
        };
        filteredItems = new FilteredList<>(sourceItems());
        itemList.setItems(new SortedList<>(filteredItems, itemComparator));
        itemList.setCellFactory(ignored -> new QuantityCell());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter(newValue));
        itemList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showItem(newValue));
        quantitySpinner.valueProperty().addListener(
                (observable, oldValue, newValue) -> saveQuantity(newValue));

        applyFilter(searchField.getText());
        if (!itemList.getItems().isEmpty()) {
            itemList.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void chooseNewItem() {
        Optional<T> selected = CatalogSelectionDialog.show(
                searchField.getScene() != null ? searchField.getScene().getWindow() : null,
                "添加库存物品",
                sourceItems(),
                item -> quantity(item.getId()) == 0,
                itemComparator);
        if (selected.isEmpty()) {
            return;
        }

        T item = selected.get();
        try {
            setQuantity(item.getId(), 1);
            if (searchField.getText().isEmpty()) {
                refreshVisibleItems();
            } else {
                searchField.clear();
            }
            itemList.getSelectionModel().select(item);
            itemList.scrollTo(item);
            saveStatus.setText("已添加并保存");
        } catch (RuntimeException exception) {
            saveStatus.setText("添加失败：" + exception.getMessage());
        }
    }

    @FXML
    public void incrementQuantity() {
        int current = quantitySpinner.getValue();
        quantitySpinner.getValueFactory().setValue(Math.min(MAX_QUANTITY, current + 1));
    }

    @FXML
    public void decrementQuantity() {
        int current = quantitySpinner.getValue();
        quantitySpinner.getValueFactory().setValue(Math.max(0, current - 1));
    }

    private void applyFilter(String text) {
        searchText = text != null ? text.trim().toLowerCase(Locale.ROOT) : "";
        refreshVisibleItems();
    }

    private void refreshVisibleItems() {
        filteredItems.setPredicate(item -> quantity(item.getId()) > 0
                && (searchText.isEmpty()
                || contains(item.getId(), searchText)
                || contains(item.getLabel(), searchText)
                || contains(item.getDesc(), searchText)));
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void showItem(T item) {
        updatingSpinner = true;
        try {
            if (item == null) {
                itemImage.setImage(null);
                itemName.setText("");
                itemId.setText("");
                quantitySpinner.getValueFactory().setValue(0);
                quantitySpinner.setDisable(true);
            } else {
                itemImage.setImage(CatalogImageService.imageFor(item));
                itemName.setText(item.getDisplayName());
                itemId.setText(item.getId());
                quantitySpinner.setDisable(false);
                quantitySpinner.getValueFactory().setValue(quantity(item.getId()));
            }
            saveStatus.setText("");
        } finally {
            updatingSpinner = false;
        }
    }

    private void saveQuantity(Integer amount) {
        T selected = itemList.getSelectionModel().getSelectedItem();
        if (updatingSpinner || selected == null || amount == null) {
            return;
        }
        try {
            setQuantity(selected.getId(), amount);
            refreshVisibleItems();
            itemList.refresh();
            if (amount > 0) {
                itemList.getSelectionModel().select(selected);
                saveStatus.setText("已保存");
            }
        } catch (RuntimeException exception) {
            saveStatus.setText("保存失败：" + exception.getMessage());
            updatingSpinner = true;
            quantitySpinner.getValueFactory().setValue(quantity(selected.getId()));
            updatingSpinner = false;
        }
    }

    private final class QuantityCell extends ListCell<T> {
        private final ImageView image = new ImageView();
        private final Label name = new Label();
        private final Label amount = new Label();
        private final HBox content = new HBox(8, image, name, amount);

        private QuantityCell() {
            image.setFitWidth(24);
            image.setFitHeight(24);
            image.setPreserveRatio(true);
            content.setAlignment(Pos.CENTER_LEFT);
            amount.setStyle("-fx-text-fill: #666666; -fx-font-weight: bold;");
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            image.setImage(CatalogImageService.imageFor(item));
            name.setText(item.getDisplayName());
            amount.setText("× " + quantity(item.getId()));
            setGraphic(content);
        }
    }
}
