package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.ElementClassificationPolicy;
import com.github.alphardpaarthurnax.bohcalculator.database.SdeDataService;
import com.github.alphardpaarthurnax.bohcalculator.model.Thing;
import javafx.collections.ObservableList;

public final class ThingDataService extends SdeDataService<Thing> {
    private static final ThingDataService INSTANCE = new ThingDataService();

    private ThingDataService() {
        super("things.json", "things", Thing.class,
                item -> ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
    }

    public static ThingDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Thing> getThings() {
        return getItems();
    }
}
