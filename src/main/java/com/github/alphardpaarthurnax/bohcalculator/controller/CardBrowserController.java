package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.service.CardDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CardBrowserSupport;
import javafx.collections.ObservableList;

public final class CardBrowserController extends CardBrowserSupport<Card> {
    @Override
    protected ObservableList<Card> sourceCards() {
        return CardDataService.getInstance().getCards();
    }
}
