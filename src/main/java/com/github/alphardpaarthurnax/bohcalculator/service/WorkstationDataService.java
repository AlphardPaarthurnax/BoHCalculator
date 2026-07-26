package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;
import javafx.collections.ObservableList;

public final class WorkstationDataService extends SdeDataService<Workstation> {
    private static final WorkstationDataService INSTANCE = new WorkstationDataService();

    private WorkstationDataService() {
        super("workstations.json", "workstations", Workstation.class, ignored -> true);
    }

    public static WorkstationDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Workstation> getWorkstations() {
        return getItems();
    }
}
