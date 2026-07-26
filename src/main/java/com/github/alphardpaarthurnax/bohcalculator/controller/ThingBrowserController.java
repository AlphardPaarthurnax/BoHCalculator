package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Thing;
import com.github.alphardpaarthurnax.bohcalculator.service.ThingDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CardBrowserSupport;
import javafx.collections.ObservableList;

public final class ThingBrowserController extends CardBrowserSupport<Thing> {
    @Override
    protected ObservableList<Thing> sourceCards() {
        return ThingDataService.getInstance().getThings();
    }
}
