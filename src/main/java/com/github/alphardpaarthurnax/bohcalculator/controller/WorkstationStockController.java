package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;
import com.github.alphardpaarthurnax.bohcalculator.service.WorkstationStockService;
import com.github.alphardpaarthurnax.bohcalculator.utils.UnlockStockSupport;
import javafx.collections.ObservableList;

public final class WorkstationStockController extends UnlockStockSupport<Workstation> {
    private final WorkstationStockService service = WorkstationStockService.getInstance();

    @Override protected ObservableList<Workstation> sourceItems() { return service.getWorkstations(); }
    @Override protected boolean isUnlocked(String id) { return service.isUnlocked(id); }
    @Override protected void setUnlocked(String id, boolean value) { service.setUnlocked(id, value); }
}
