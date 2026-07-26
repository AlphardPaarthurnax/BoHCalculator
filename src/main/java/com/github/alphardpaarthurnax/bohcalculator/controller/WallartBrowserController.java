package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Wallart;
import com.github.alphardpaarthurnax.bohcalculator.service.WallartDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CardBrowserSupport;
import javafx.collections.ObservableList;

public final class WallartBrowserController extends CardBrowserSupport<Wallart> {
    @Override
    protected ObservableList<Wallart> sourceCards() {
        return WallartDataService.getInstance().getWallarts();
    }
}
