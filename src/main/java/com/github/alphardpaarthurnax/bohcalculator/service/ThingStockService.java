package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.QuantityStockStore;
import com.github.alphardpaarthurnax.bohcalculator.model.Thing;
import javafx.collections.ObservableList;

public final class ThingStockService {
    private static final ThingStockService INSTANCE = new ThingStockService();
    private final QuantityStockStore<Thing> stock = new QuantityStockStore<>(
            "things.json", ThingDataService.getInstance().getThings());

    private ThingStockService() {
    }

    public static ThingStockService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Thing> getThings() {
        return stock.catalog();
    }

    public int getQuantity(String id) {
        return stock.quantity(id);
    }

    public void setQuantity(String id, int amount) {
        stock.setQuantity(id, amount);
    }
}
