package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Comfort;
import com.github.alphardpaarthurnax.bohcalculator.service.ComfortDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CardBrowserSupport;
import javafx.collections.ObservableList;

public final class ComfortBrowserController extends CardBrowserSupport<Comfort> {
    @Override
    protected ObservableList<Comfort> sourceCards() {
        return ComfortDataService.getInstance().getComforts();
    }
}
