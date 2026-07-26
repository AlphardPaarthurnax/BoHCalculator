package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.ElementClassificationPolicy;
import com.github.alphardpaarthurnax.bohcalculator.database.SdeDataService;
import com.github.alphardpaarthurnax.bohcalculator.model.Wallart;
import javafx.collections.ObservableList;

public final class WallartDataService extends SdeDataService<Wallart> {
    private static final WallartDataService INSTANCE = new WallartDataService();

    private WallartDataService() {
        super("wallarts.json", "wallarts", Wallart.class,
                item -> ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
    }

    public static WallartDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Wallart> getWallarts() {
        return getItems();
    }
}
