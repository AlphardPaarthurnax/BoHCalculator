package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Comfort;
import com.github.alphardpaarthurnax.bohcalculator.service.ComfortStockService;
import com.github.alphardpaarthurnax.bohcalculator.utils.UnlockStockSupport;
import javafx.collections.ObservableList;

public final class ComfortStockController extends UnlockStockSupport<Comfort> {
    private final ComfortStockService service = ComfortStockService.getInstance();

    @Override protected ObservableList<Comfort> sourceItems() { return service.getComforts(); }
    @Override protected boolean isUnlocked(String id) { return service.isUnlocked(id); }
    @Override protected void setUnlocked(String id, boolean value) { service.setUnlocked(id, value); }
}
