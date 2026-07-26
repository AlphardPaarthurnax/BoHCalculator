package com.github.alphardpaarthurnax.bohcalculator.utils;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class CardBrowserSupport<T extends Card> {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<T> cardList;

    @FXML
    private ImageView cardImage;

    @FXML
    private Label cardName;

    @FXML
    private Label cardId;

    @FXML
    private Label cardSource;

    @FXML
    private TextFlow cardDescription;

    @FXML
    private TableView<AspectEntry> aspectTable;

    private final CatalogReferenceIndex references = CatalogReferenceIndex.getInstance();
    private FilteredList<T> filteredCards;

    @FXML
    public void initialize() {
        Collator collator = Collator.getInstance(Locale.CHINA);
        Comparator<T> cardComparator = (a, b) -> {
            boolean aHasLabel = a.getLabel() != null && !a.getLabel().isEmpty();
            boolean bHasLabel = b.getLabel() != null && !b.getLabel().isEmpty();
            if (aHasLabel != bHasLabel) {
                return aHasLabel ? -1 : 1;
            }
            return collator.compare(a.getDisplayName(), b.getDisplayName());
        };

        filteredCards = new FilteredList<>(sourceCards());
        SortedList<T> sortedList = new SortedList<>(filteredCards, cardComparator);
        sortedList.comparatorProperty().set(cardComparator);

        cardList.setItems(sortedList);

        cardList.setCellFactory(ignored -> new CatalogListCell<>());

        setupAspectTable();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = (newVal != null) ? newVal.toLowerCase(Locale.ROOT) : "";
            filteredCards.setPredicate(card -> {
                if (filter.isEmpty()) return true;
                if (card.getId() != null && card.getId().toLowerCase(Locale.ROOT).contains(filter)) return true;
                if (card.getLabel() != null && card.getLabel().toLowerCase(Locale.ROOT).contains(filter)) return true;
                if (card.getDesc() != null && card.getDesc().toLowerCase(Locale.ROOT).contains(filter)) return true;
                return false;
            });
        });

        cardList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cardName.setText(newVal.getDisplayName());
                cardId.setText(newVal.getId() != null ? newVal.getId() : "");
                cardSource.setText(newVal.getSourceFile() != null ? newVal.getSourceFile() : "");
                cardDescription.getChildren().setAll(RichDescriptionRenderer.render(newVal.getDesc()));
                cardImage.setImage(CatalogImageService.imageFor(newVal));
                populateAspectTable(newVal);
            } else {
                cardName.setText("");
                cardId.setText("");
                cardSource.setText("");
                cardDescription.getChildren().clear();
                cardImage.setImage(null);
                aspectTable.getItems().clear();
            }
        });

        if (!sortedList.isEmpty()) {
            cardList.getSelectionModel().select(0);
        }
    }

    protected abstract ObservableList<T> sourceCards();

    @SuppressWarnings("unchecked")
    private void setupAspectTable() {
        TableColumn<AspectEntry, Void> iconCol = new TableColumn<>("图标");
        iconCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(24);
                iv.setFitHeight(24);
                iv.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Aspect a = getTableRow().getItem().aspect;
                    iv.setImage(a != null ? CatalogImageService.imageFor(a) : null);
                    setGraphic(iv);
                }
            }
        });
        iconCol.setPrefWidth(40);
        iconCol.setMinWidth(40);
        iconCol.setMaxWidth(40);

        TableColumn<AspectEntry, String> nameCol = new TableColumn<>("性相");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayName()));
        nameCol.setPrefWidth(150);

        TableColumn<AspectEntry, String> amountCol = new TableColumn<>("等级");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().amount)));
        amountCol.setPrefWidth(60);

        TableColumn<AspectEntry, String> sourceCol = new TableColumn<>("来源");
        sourceCol.setCellValueFactory(data -> {
            Aspect a = data.getValue().aspect;
            return new SimpleStringProperty(a != null ? a.getSourceFile() : "");
        });
        sourceCol.setPrefWidth(200);

        aspectTable.getColumns().addAll(iconCol, nameCol, amountCol, sourceCol);
        aspectTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void populateAspectTable(Card card) {
        List<AspectEntry> entries = new ArrayList<>();
        if (card.getAspects() != null) {
            for (Map.Entry<String, Integer> entry : card.getAspects().entrySet()) {
                Aspect aspect = references.aspects().get(entry.getKey());
                if (aspect != null) {
                    entries.add(new AspectEntry(entry.getKey(), aspect, entry.getValue()));
                }
            }
        }
        aspectTable.getItems().setAll(entries);
    }

    private static class AspectEntry {
        final String id;
        final Aspect aspect;
        final int amount;

        AspectEntry(String id, Aspect aspect, int amount) {
            this.id = id;
            this.aspect = aspect;
            this.amount = amount;
        }

        String getDisplayName() {
            return aspect != null ? aspect.getDisplayName() : id;
        }
    }

}
