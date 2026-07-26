package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Thing;
import com.github.alphardpaarthurnax.bohcalculator.service.ThingStockService;
import com.github.alphardpaarthurnax.bohcalculator.utils.QuantityStockSupport;
import javafx.collections.ObservableList;

public final class ThingStockController extends QuantityStockSupport<Thing> {
    private final ThingStockService service = ThingStockService.getInstance();

    @Override protected ObservableList<Thing> sourceItems() { return service.getThings(); }
    @Override protected int quantity(String id) { return service.getQuantity(id); }
    @Override protected void setQuantity(String id, int amount) { service.setQuantity(id, amount); }
}
