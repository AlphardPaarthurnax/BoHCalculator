package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.UnlockStockStore;
import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;
import javafx.collections.ObservableList;

public final class WorkstationStockService {
    private static final WorkstationStockService INSTANCE = new WorkstationStockService();
    private final UnlockStockStore<Workstation> stock = new UnlockStockStore<>(
            "workstations.json", WorkstationDataService.getInstance().getWorkstations());

    private WorkstationStockService() {
    }

    public static WorkstationStockService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Workstation> getWorkstations() {
        return stock.catalog();
    }

    public boolean isUnlocked(String id) {
        return stock.isUnlocked(id);
    }

    public void setUnlocked(String id, boolean unlocked) {
        stock.setUnlocked(id, unlocked);
    }
}
