package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.service.AspectDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.CardDataService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CardBrowserController {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<Card> cardList;

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

    private Map<String, Aspect> aspectMap;
    private FilteredList<Card> filteredCards;

    @FXML
    public void initialize() {
        aspectMap = AspectDataService.getInstance().getAspects().stream()
                .collect(Collectors.toMap(Aspect::getId, a -> a));

        Collator collator = Collator.getInstance(Locale.CHINA);
        Comparator<Card> cardComparator = (a, b) -> {
            boolean aHasLabel = a.getLabel() != null && !a.getLabel().isEmpty();
            boolean bHasLabel = b.getLabel() != null && !b.getLabel().isEmpty();
            if (aHasLabel != bHasLabel) {
                return aHasLabel ? -1 : 1;
            }
            return collator.compare(a.getDisplayName(), b.getDisplayName());
        };

        filteredCards = new FilteredList<>(CardDataService.getInstance().getCards());
        SortedList<Card> sortedList = new SortedList<>(filteredCards, cardComparator);
        sortedList.comparatorProperty().set(cardComparator);

        cardList.setItems(sortedList);

        cardList.setCellFactory(lv -> new ListCell<Card>() {
            private final ImageView iv = new ImageView();
            private final Label label = new Label();
            private final HBox hbox = new HBox(8);

            {
                iv.setFitWidth(24);
                iv.setFitHeight(24);
                iv.setPreserveRatio(true);
                hbox.getChildren().addAll(iv, label);
                hbox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Card item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    iv.setImage(item.getImage());
                    label.setText(item.getDisplayName());
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

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
                cardDescription.getChildren().setAll(parseDescription(newVal.getDesc()));
                cardImage.setImage(newVal.getImage());
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
                    iv.setImage(a != null ? a.getImage() : null);
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
        aspectTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void populateAspectTable(Card card) {
        List<AspectEntry> entries = new ArrayList<>();
        if (card.getAspects() != null) {
            for (Map.Entry<String, Integer> entry : card.getAspects().entrySet()) {
                Aspect aspect = aspectMap.get(entry.getKey());
                if (aspect != null) {
                    entries.add(new AspectEntry(aspect, entry.getValue()));
                }
            }
        }
        aspectTable.getItems().setAll(entries);
    }

    private static class AspectEntry {
        final Aspect aspect;
        final int amount;

        AspectEntry(Aspect aspect, int amount) {
            this.aspect = aspect;
            this.amount = amount;
        }

        String getDisplayName() {
            return aspect != null ? aspect.getDisplayName() : "";
        }
    }

    private List<Node> parseDescription(String desc) {
        List<Node> nodes = new ArrayList<>();
        if (desc == null || desc.isEmpty()) return nodes;

        Pattern pattern = Pattern.compile("<b>(.*?)</b>|<i>(.*?)</i>|<sprite name=(.*?)>");
        Matcher matcher = pattern.matcher(desc);

        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                nodes.add(new Text(desc.substring(lastEnd, matcher.start())));
            }
            if (matcher.group(1) != null) {
                Text text = new Text(matcher.group(1));
                text.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
                nodes.add(text);
            } else if (matcher.group(2) != null) {
                Text text = new Text(matcher.group(2));
                text.setStyle("-fx-fill: #c41d7f; -fx-font-style: italic;");
                nodes.add(text);
            } else if (matcher.group(3) != null) {
                String spriteName = matcher.group(3);
                ImageView iv = new ImageView();
                iv.setFitWidth(20);
                iv.setFitHeight(20);
                iv.setPreserveRatio(true);
                iv.setTranslateY(3);
                InputStream is = getClass().getResourceAsStream("/assets/images/aspects/" + spriteName + ".png");
                if (is != null) {
                    iv.setImage(new Image(is));
                }
                nodes.add(iv);
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < desc.length()) {
            nodes.add(new Text(desc.substring(lastEnd)));
        }
        return nodes;
    }
}
