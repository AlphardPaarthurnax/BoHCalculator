package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.collections.ListChangeListener;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CatalogReferenceIndex {
    private static final CatalogReferenceIndex INSTANCE = new CatalogReferenceIndex();

    private Map<String, Aspect> aspects = Map.of();
    private Map<String, Card> cards = Map.of();
    private Map<String, CatalogItem> all = Map.of();

    private CatalogReferenceIndex() {
        AspectDataService.getInstance().getAspects().addListener(
                (ListChangeListener<Aspect>) change -> refresh());
        CardDataService.getInstance().getCards().addListener(
                (ListChangeListener<Card>) change -> refresh());
        refresh();
    }

    public static CatalogReferenceIndex getInstance() {
        return INSTANCE;
    }

    public Map<String, Aspect> aspects() {
        return aspects;
    }

    public Map<String, Card> cards() {
        return cards;
    }

    public Map<String, CatalogItem> all() {
        return all;
    }

    private void refresh() {
        Map<String, Aspect> aspectIndex = new LinkedHashMap<>();
        AspectDataService.getInstance().getAspects().forEach(item -> aspectIndex.put(item.getId(), item));
        Map<String, Card> cardIndex = new LinkedHashMap<>();
        CardDataService.getInstance().getCards().forEach(item -> cardIndex.put(item.getId(), item));
        Map<String, CatalogItem> combined = new LinkedHashMap<>();
        combined.putAll(aspectIndex);
        combined.putAll(cardIndex);
        aspects = Map.copyOf(aspectIndex);
        cards = Map.copyOf(cardIndex);
        all = Map.copyOf(combined);
    }
}
