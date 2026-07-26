package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import javafx.collections.ObservableList;

public final class ElementDataService extends SdeDataService<Element> {
    private static final ElementDataService INSTANCE = new ElementDataService();

    private ElementDataService() {
        super("elements.json", "elements", Element.class, ignored -> true);
    }

    public static ElementDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Element> getElements() {
        return getItems();
    }
}
