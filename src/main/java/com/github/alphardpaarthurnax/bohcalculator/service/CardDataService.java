package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.ElementClassificationPolicy;
import com.github.alphardpaarthurnax.bohcalculator.database.SdeDataService;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import javafx.collections.ObservableList;

public final class CardDataService extends SdeDataService<Card> {
    private static final CardDataService INSTANCE = new CardDataService();

    private CardDataService() {
        super("cards.json", "cards", Card.class,
                card -> ElementClassificationPolicy.hasNormalImage(card.getRowenariumImageSrc()));
    }

    public static CardDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Card> getCards() {
        return getItems();
    }
}
