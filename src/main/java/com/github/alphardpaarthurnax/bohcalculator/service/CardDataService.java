package com.github.alphardpaarthurnax.bohcalculator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class CardDataService {

    private static final String SDE_PATH = "/assets/sde/cards.json";

    private static CardDataService instance;
    private final ObservableList<Card> cachedCards = FXCollections.observableArrayList();

    private CardDataService() {
        cachedCards.addAll(loadFromSde());
    }

    public static CardDataService getInstance() {
        if (instance == null) {
            instance = new CardDataService();
        }
        return instance;
    }

    public ObservableList<Card> getCards() {
        return cachedCards;
    }

    public void reload() {
        cachedCards.clear();
        cachedCards.addAll(loadFromSde());
    }

    private List<Card> loadFromSde() {
        try (InputStream is = getClass().getResourceAsStream(SDE_PATH)) {
            if (is == null) {
                System.err.println("SDE file not found: " + SDE_PATH + " — run Data Generator first.");
                return List.of();
            }
            ObjectMapper mapper = new ObjectMapper();
            Map<String, List<Card>> data = mapper.readValue(is,
                    new TypeReference<Map<String, List<Card>>>() {});
            List<Card> cards = data.get("cards");
            if (cards != null) {
                for (Card c : cards) {
                    if (c.getImagePath() != null) {
                        InputStream imgStream = getClass().getResourceAsStream(c.getImagePath());
                        if (imgStream != null) {
                            c.setImage(new Image(imgStream));
                        }
                    }
                }
            }
            return cards != null ? cards : List.of();
        } catch (Exception e) {
            System.err.println("Failed to load SDE: " + e.getMessage());
            return List.of();
        }
    }
}
