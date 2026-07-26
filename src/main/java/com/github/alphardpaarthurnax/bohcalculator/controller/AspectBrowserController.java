package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.service.AspectDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.CatalogImageService;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

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
        Collator collator = Collator.getInstance(Locale.CHINA);
        Comparator<Aspect> elementComparator = (a, b) -> {
            int aGroup = sortGroup(a);
            int bGroup = sortGroup(b);
            if (aGroup != bGroup) {
                return Integer.compare(aGroup, bGroup);
            }
            return collator.compare(a.getDisplayName(), b.getDisplayName());
        };

        filteredElements = new FilteredList<>(AspectDataService.getInstance().getAspects());
        SortedList<Aspect> sortedList = new SortedList<>(filteredElements, elementComparator);
        sortedList.comparatorProperty().set(elementComparator);

        elementList.setItems(sortedList);

        elementList.setCellFactory(ignored -> new CatalogListCell<>());

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
                elementDescription.getChildren().setAll(RichDescriptionRenderer.render(newVal.getDesc()));
                elementImage.setImage(CatalogImageService.imageFor(newVal));
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

    private int sortGroup(Aspect aspect) {
        boolean hasLabel = aspect.getLabel() != null && !aspect.getLabel().isBlank();
        if (!hasLabel) {
            return 2;
        }
        boolean salonTransition = aspect.getId() != null
                && aspect.getId().startsWith("slnstg.")
                && ("……".equals(aspect.getLabel()) || "......".equals(aspect.getLabel()));
        return salonTransition ? 1 : 0;
    }

}
