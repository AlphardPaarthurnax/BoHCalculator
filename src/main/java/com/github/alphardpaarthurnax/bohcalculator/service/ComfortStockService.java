package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.UnlockStockStore;
import com.github.alphardpaarthurnax.bohcalculator.model.Comfort;
import javafx.collections.ObservableList;

public final class ComfortStockService {
    private static final ComfortStockService INSTANCE = new ComfortStockService();
    private final UnlockStockStore<Comfort> stock = new UnlockStockStore<>(
            "comforts.json", ComfortDataService.getInstance().getComforts());

    private ComfortStockService() {
    }

    public static ComfortStockService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Comfort> getComforts() {
        return stock.catalog();
    }

    public boolean isUnlocked(String id) {
        return stock.isUnlocked(id);
    }

    public void setUnlocked(String id, boolean unlocked) {
        stock.setUnlocked(id, unlocked);
    }
}
