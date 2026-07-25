package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.service.AspectDataService;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AspectBrowserController {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<Aspect> elementList;

    @FXML
    private ImageView elementImage;

    @FXML
    private Label elementName;

    @FXML
    private Label elementId;

    @FXML
    private Label elementSource;

    @FXML
    private TextFlow elementDescription;

    private FilteredList<Aspect> filteredElements;

    @FXML
    public void initialize() {
        AspectDataService dataService = new AspectDataService();
        var allElements = dataService.loadAllElements();

        Collator collator = Collator.getInstance(Locale.CHINA);
        Comparator<Aspect> elementComparator = (a, b) -> {
            boolean aHasLabel = a.getLabel() != null && !a.getLabel().isEmpty();
            boolean bHasLabel = b.getLabel() != null && !b.getLabel().isEmpty();
            if (aHasLabel != bHasLabel) {
                return aHasLabel ? -1 : 1;
            }
            return collator.compare(a.getDisplayName(), b.getDisplayName());
        };

        filteredElements = new FilteredList<>(FXCollections.observableArrayList(allElements));
        SortedList<Aspect> sortedList = new SortedList<>(filteredElements, elementComparator);
        sortedList.comparatorProperty().set(elementComparator);

        elementList.setItems(sortedList);

        elementList.setCellFactory(lv -> new ListCell<Aspect>() {
            private final ImageView imageView = new ImageView();
            private final Label label = new Label();
            private final HBox hbox = new HBox(8);

            {
                imageView.setFitWidth(24);
                imageView.setFitHeight(24);
                imageView.setPreserveRatio(true);
                hbox.getChildren().addAll(imageView, label);
                hbox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Aspect item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    imageView.setImage(item.getImage());
                    label.setText(item.getDisplayName());
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = (newVal != null) ? newVal.toLowerCase(Locale.ROOT) : "";
            filteredElements.setPredicate(element -> {
                if (filter.isEmpty()) {
                    return true;
                }
                if (element.getId() != null && element.getId().toLowerCase(Locale.ROOT).contains(filter)) {
                    return true;
                }
                if (element.getLabel() != null && element.getLabel().toLowerCase(Locale.ROOT).contains(filter)) {
                    return true;
                }
                if (element.getDesc() != null && element.getDesc().toLowerCase(Locale.ROOT).contains(filter)) {
                    return true;
                }
                return false;
            });
        });

        elementList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                elementName.setText(newVal.getDisplayName());
                elementId.setText(newVal.getId() != null ? newVal.getId() : "");
                elementSource.setText(newVal.getSourceFile() != null ? newVal.getSourceFile() : "");
                elementDescription.getChildren().setAll(parseDescription(newVal.getDesc()));
                elementImage.setImage(newVal.getImage());
            } else {
                elementName.setText("");
                elementId.setText("");
                elementSource.setText("");
                elementDescription.getChildren().clear();
                elementImage.setImage(null);
            }
        });

        if (!sortedList.isEmpty()) {
            elementList.getSelectionModel().select(0);
        }
    }

    private List<Node> parseDescription(String desc) {
        List<Node> nodes = new ArrayList<>();
        if (desc == null || desc.isEmpty()) {
            return nodes;
        }

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
