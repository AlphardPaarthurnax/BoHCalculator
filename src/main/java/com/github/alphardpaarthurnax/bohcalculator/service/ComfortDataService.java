package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.ElementClassificationPolicy;
import com.github.alphardpaarthurnax.bohcalculator.database.SdeDataService;
import com.github.alphardpaarthurnax.bohcalculator.model.Comfort;
import javafx.collections.ObservableList;

public final class ComfortDataService extends SdeDataService<Comfort> {
    private static final ComfortDataService INSTANCE = new ComfortDataService();

    private ComfortDataService() {
        super("comforts.json", "comforts", Comfort.class,
                item -> ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
    }

    public static ComfortDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Comfort> getComforts() {
        return getItems();
    }
}
