package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import javafx.collections.ObservableList;

public final class AspectDataService extends SdeDataService<Aspect> {
    private static final AspectDataService INSTANCE = new AspectDataService();

    private AspectDataService() {
        super("aspects.json", "aspects", Aspect.class,
                aspect -> ElementClassificationPolicy.hasNormalImage(aspect.getRowenariumImageSrc()));
    }

    public static AspectDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Aspect> getAspects() {
        return getItems();
    }
}
