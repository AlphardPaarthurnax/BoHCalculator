package com.github.alphardpaarthurnax.bohcalculator.utils;

import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public abstract class UnlockStockSupport<T extends CatalogItem> {
    @FXML protected TextField searchField;
    @FXML protected ListView<T> unlockedList;
    @FXML protected ListView<T> lockedList;
    @FXML protected Button unlockButton;
    @FXML protected Button lockButton;
    @FXML protected Label saveStatus;

    private FilteredList<T> unlockedItems;
    private FilteredList<T> lockedItems;
    private String searchText = "";

    protected abstract ObservableList<T> sourceItems();

    protected abstract boolean isUnlocked(String id);

    protected abstract void setUnlocked(String id, boolean unlocked);

    @FXML
    public void initialize() {
        Collator collator = Collator.getInstance(Locale.CHINA);
        Comparator<T> comparator = (left, right) -> {
            int byName = collator.compare(left.getDisplayName(), right.getDisplayName());
            if (byName != 0) {
                return byName;
            }
            String leftId = left.getId() != null ? left.getId() : "";
            String rightId = right.getId() != null ? right.getId() : "";
            return leftId.compareTo(rightId);
        };
        unlockedItems = new FilteredList<>(sourceItems());
        lockedItems = new FilteredList<>(sourceItems());
        unlockedList.setItems(new SortedList<>(unlockedItems, comparator));
        lockedList.setItems(new SortedList<>(lockedItems, comparator));
        unlockedList.setCellFactory(ignored -> new CatalogListCell<>());
        lockedList.setCellFactory(ignored -> new CatalogListCell<>());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue != null ? newValue.trim().toLowerCase(Locale.ROOT) : "";
            refilter();
        });
        unlockedList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, item) -> {
            if (item != null) {
                lockedList.getSelectionModel().clearSelection();
            }
            updateButtons();
        });
        lockedList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, item) -> {
            if (item != null) {
                unlockedList.getSelectionModel().clearSelection();
            }
            updateButtons();
        });
        refilter();
        updateButtons();
    }

    @FXML
    public void unlockSelected() {
        changeState(lockedList.getSelectionModel().getSelectedItem(), true);
    }

    @FXML
    public void lockSelected() {
        changeState(unlockedList.getSelectionModel().getSelectedItem(), false);
    }

    private void changeState(T item, boolean unlocked) {
        if (item == null) {
            return;
        }
        try {
            setUnlocked(item.getId(), unlocked);
            saveStatus.setText("已保存：" + item.getDisplayName());
            refilter();
            unlockedList.getSelectionModel().clearSelection();
            lockedList.getSelectionModel().clearSelection();
            updateButtons();
        } catch (RuntimeException exception) {
            saveStatus.setText("保存失败：" + exception.getMessage());
        }
    }

    private void refilter() {
        unlockedItems.setPredicate(item -> isUnlocked(item.getId()) && matchesSearch(item));
        lockedItems.setPredicate(item -> !isUnlocked(item.getId()) && matchesSearch(item));
    }

    private boolean matchesSearch(T item) {
        return searchText.isEmpty()
                || contains(item.getId())
                || contains(item.getLabel())
                || contains(item.getDesc());
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(searchText);
    }

    private void updateButtons() {
        unlockButton.setDisable(lockedList.getSelectionModel().getSelectedItem() == null);
        lockButton.setDisable(unlockedList.getSelectionModel().getSelectedItem() == null);
    }
}
