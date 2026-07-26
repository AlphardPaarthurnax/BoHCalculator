package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import javafx.collections.ObservableList;

public final class VerbDataService extends SdeDataService<Verb> {
    private static final VerbDataService INSTANCE = new VerbDataService();

    private VerbDataService() {
        super("verbs.json", "verbs", Verb.class, ignored -> true);
    }

    public static VerbDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Verb> getVerbs() {
        return getItems();
    }
}
