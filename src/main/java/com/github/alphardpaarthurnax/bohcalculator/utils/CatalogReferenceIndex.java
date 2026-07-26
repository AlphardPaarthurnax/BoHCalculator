package com.github.alphardpaarthurnax.bohcalculator.utils;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import com.github.alphardpaarthurnax.bohcalculator.service.AspectDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.BookDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.CardDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.ComfortDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.ThingDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.WallartDataService;
import javafx.collections.ListChangeListener;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CatalogReferenceIndex {
    private static final CatalogReferenceIndex INSTANCE = new CatalogReferenceIndex();

    private Map<String, Aspect> aspects = Map.of();
    private Map<String, Card> cardsAndItems = Map.of();
    private Map<String, CatalogItem> all = Map.of();

    private CatalogReferenceIndex() {
        AspectDataService.getInstance().getAspects().addListener(
                (ListChangeListener<Aspect>) change -> refresh());
        ListChangeListener<Card> cardListener = change -> refresh();
        CardDataService.getInstance().getCards().addListener(cardListener);
        BookDataService.getInstance().getBooks().addListener(cardListener);
        WallartDataService.getInstance().getWallarts().addListener(cardListener);
        ComfortDataService.getInstance().getComforts().addListener(cardListener);
        ThingDataService.getInstance().getThings().addListener(cardListener);
        refresh();
    }

    public static CatalogReferenceIndex getInstance() {
        return INSTANCE;
    }

    public Map<String, Aspect> aspects() {
        return aspects;
    }

    public Map<String, Card> cardsAndItems() {
        return cardsAndItems;
    }

    public Map<String, CatalogItem> all() {
        return all;
    }

    private void refresh() {
        Map<String, Aspect> aspectIndex = new LinkedHashMap<>();
        AspectDataService.getInstance().getAspects().forEach(item -> aspectIndex.put(item.getId(), item));
        Map<String, Card> cardIndex = new LinkedHashMap<>();
        CardDataService.getInstance().getCards().forEach(item -> cardIndex.put(item.getId(), item));
        Map<String, Card> cardAndItemIndex = new LinkedHashMap<>(cardIndex);
        BookDataService.getInstance().getBooks().forEach(item -> cardAndItemIndex.put(item.getId(), item));
        WallartDataService.getInstance().getWallarts().forEach(item -> cardAndItemIndex.put(item.getId(), item));
        ComfortDataService.getInstance().getComforts().forEach(item -> cardAndItemIndex.put(item.getId(), item));
        ThingDataService.getInstance().getThings().forEach(item -> cardAndItemIndex.put(item.getId(), item));
        Map<String, CatalogItem> combined = new LinkedHashMap<>();
        combined.putAll(aspectIndex);
        combined.putAll(cardAndItemIndex);
        aspects = Map.copyOf(aspectIndex);
        cardsAndItems = Map.copyOf(cardAndItemIndex);
        all = Map.copyOf(combined);
    }
}
