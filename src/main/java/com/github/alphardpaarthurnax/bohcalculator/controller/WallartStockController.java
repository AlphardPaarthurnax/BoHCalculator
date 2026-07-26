package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Wallart;
import com.github.alphardpaarthurnax.bohcalculator.service.WallartStockService;
import com.github.alphardpaarthurnax.bohcalculator.utils.UnlockStockSupport;
import javafx.collections.ObservableList;

public final class WallartStockController extends UnlockStockSupport<Wallart> {
    private final WallartStockService service = WallartStockService.getInstance();

    @Override protected ObservableList<Wallart> sourceItems() { return service.getWallarts(); }
    @Override protected boolean isUnlocked(String id) { return service.isUnlocked(id); }
    @Override protected void setUnlocked(String id, boolean value) { service.setUnlocked(id, value); }
}
