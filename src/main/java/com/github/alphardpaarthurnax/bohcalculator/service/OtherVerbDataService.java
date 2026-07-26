package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.SdeDataService;
import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import javafx.collections.ObservableList;

public final class OtherVerbDataService extends SdeDataService<Verb> {
    private static final OtherVerbDataService INSTANCE = new OtherVerbDataService();

    private OtherVerbDataService() {
        super("other-verbs.json", "otherVerbs", Verb.class, ignored -> true);
    }

    public static OtherVerbDataService getInstance() { return INSTANCE; }
    public ObservableList<Verb> getOtherVerbs() { return getItems(); }
}
