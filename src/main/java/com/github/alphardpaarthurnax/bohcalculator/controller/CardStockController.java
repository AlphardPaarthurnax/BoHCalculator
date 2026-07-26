package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.service.CardStockService;
import com.github.alphardpaarthurnax.bohcalculator.utils.QuantityStockSupport;
import javafx.collections.ObservableList;

public final class CardStockController extends QuantityStockSupport<Card> {
    private final CardStockService service = CardStockService.getInstance();

    @Override protected ObservableList<Card> sourceItems() { return service.getCards(); }
    @Override protected int quantity(String id) { return service.getQuantity(id); }
    @Override protected void setQuantity(String id, int amount) { service.setQuantity(id, amount); }
}
