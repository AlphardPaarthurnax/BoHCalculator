package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.UnlockStockStore;
import com.github.alphardpaarthurnax.bohcalculator.model.Wallart;
import javafx.collections.ObservableList;

public final class WallartStockService {
    private static final WallartStockService INSTANCE = new WallartStockService();
    private final UnlockStockStore<Wallart> stock = new UnlockStockStore<>(
            "wallarts.json", WallartDataService.getInstance().getWallarts());

    private WallartStockService() {
    }

    public static WallartStockService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Wallart> getWallarts() {
        return stock.catalog();
    }

    public boolean isUnlocked(String id) {
        return stock.isUnlocked(id);
    }

    public void setUnlocked(String id, boolean unlocked) {
        stock.setUnlocked(id, unlocked);
    }
}
