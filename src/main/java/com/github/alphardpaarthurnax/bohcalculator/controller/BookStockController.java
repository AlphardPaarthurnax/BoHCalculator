package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import com.github.alphardpaarthurnax.bohcalculator.service.BookStockService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogListCell;
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

public final class BookStockController {
    @FXML private TextField searchField;
    @FXML private ListView<Book> lockedList;
    @FXML private ListView<Book> unlockedList;
    @FXML private ListView<Book> masteredList;
    @FXML private Button unlockButton;
    @FXML private Button lockButton;
    @FXML private Button masterButton;
    @FXML private Button unmasterButton;
    @FXML private Label saveStatus;

    private final BookStockService service = BookStockService.getInstance();
    private FilteredList<Book> locked;
    private FilteredList<Book> unlocked;
    private FilteredList<Book> mastered;
    private String searchText = "";

    @FXML
    public void initialize() {
        Comparator<Book> comparator = comparator();
        locked = new FilteredList<>(service.getBooks());
        unlocked = new FilteredList<>(service.getBooks());
        mastered = new FilteredList<>(service.getBooks());
        lockedList.setItems(new SortedList<>(locked, comparator));
        unlockedList.setItems(new SortedList<>(unlocked, comparator));
        masteredList.setItems(new SortedList<>(mastered, comparator));
        lockedList.setCellFactory(ignored -> new CatalogListCell<>());
        unlockedList.setCellFactory(ignored -> new CatalogListCell<>());
        masteredList.setCellFactory(ignored -> new CatalogListCell<>());

        searchField.textProperty().addListener((observable, oldValue, value) -> {
            searchText = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            refilter();
        });
        lockedList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> selectionChanged(lockedList, value));
        unlockedList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> selectionChanged(unlockedList, value));
        masteredList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, value) -> selectionChanged(masteredList, value));
        refilter();
        updateButtons();
    }

    @FXML public void unlockSelected() { update(lockedList.getSelectionModel().getSelectedItem(), State.UNLOCKED); }
    @FXML public void lockSelected() { update(unlockedList.getSelectionModel().getSelectedItem(), State.LOCKED); }
    @FXML public void masterSelected() { update(unlockedList.getSelectionModel().getSelectedItem(), State.MASTERED); }
    @FXML public void unmasterSelected() { update(masteredList.getSelectionModel().getSelectedItem(), State.UNLOCKED); }

    private void update(Book book, State state) {
        if (book == null) {
            return;
        }
        try {
            switch (state) {
                case LOCKED -> service.setUnlocked(book.getId(), false);
                case UNLOCKED -> {
                    service.setUnlocked(book.getId(), true);
                    service.setMastered(book.getId(), false);
                }
                case MASTERED -> service.setMastered(book.getId(), true);
            }
            saveStatus.setText("已保存：" + book.getDisplayName() + " → " + state.label);
            clearSelections();
            refilter();
        } catch (RuntimeException exception) {
            saveStatus.setText("保存失败：" + exception.getMessage());
        }
    }

    private void selectionChanged(ListView<Book> source, Book selected) {
        if (selected != null) {
            if (source != lockedList) lockedList.getSelectionModel().clearSelection();
            if (source != unlockedList) unlockedList.getSelectionModel().clearSelection();
            if (source != masteredList) masteredList.getSelectionModel().clearSelection();
        }
        updateButtons();
    }

    private void refilter() {
        locked.setPredicate(book -> !service.isUnlocked(book.getId()) && matches(book));
        unlocked.setPredicate(book -> service.isUnlocked(book.getId())
                && !service.isMastered(book.getId()) && matches(book));
        mastered.setPredicate(book -> service.isMastered(book.getId()) && matches(book));
        updateButtons();
    }

    private boolean matches(Book book) {
        return searchText.isEmpty() || contains(book.getId()) || contains(book.getLabel()) || contains(book.getDesc());
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(searchText);
    }

    private void clearSelections() {
        lockedList.getSelectionModel().clearSelection();
        unlockedList.getSelectionModel().clearSelection();
        masteredList.getSelectionModel().clearSelection();
    }

    private void updateButtons() {
        unlockButton.setDisable(lockedList.getSelectionModel().getSelectedItem() == null);
        lockButton.setDisable(unlockedList.getSelectionModel().getSelectedItem() == null);
        masterButton.setDisable(unlockedList.getSelectionModel().getSelectedItem() == null);
        unmasterButton.setDisable(masteredList.getSelectionModel().getSelectedItem() == null);
    }

    private Comparator<Book> comparator() {
        Collator collator = Collator.getInstance(Locale.CHINA);
        return Comparator.comparing(Book::getDisplayName, collator)
                .thenComparing(Book::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    private enum State {
        LOCKED("未解锁"), UNLOCKED("已解锁、未精通"), MASTERED("已精通");
        private final String label;
        State(String label) { this.label = label; }
    }
}
