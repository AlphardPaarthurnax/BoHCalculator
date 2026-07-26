package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.QuantityStockStore;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import javafx.collections.ObservableList;

public final class CardStockService {
    private static final CardStockService INSTANCE = new CardStockService();
    private final QuantityStockStore<Card> stock = new QuantityStockStore<>(
            "cards.json", CardDataService.getInstance().getCards());

    private CardStockService() {
    }

    public static CardStockService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Card> getCards() {
        return stock.catalog();
    }

    public int getQuantity(String id) {
        return stock.quantity(id);
    }

    public void setQuantity(String id, int amount) {
        stock.setQuantity(id, amount);
    }
}
