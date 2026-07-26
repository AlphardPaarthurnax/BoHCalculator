package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.QuantityStockStore;
import com.github.alphardpaarthurnax.bohcalculator.database.SkillStockStore;
import com.github.alphardpaarthurnax.bohcalculator.database.StockRepository;
import com.github.alphardpaarthurnax.bohcalculator.craft.SkillAspectResolver;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.SkillConfiguration;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Map;

public final class CardStockService {
    private static final CardStockService INSTANCE = new CardStockService();
    private final StockRepository repository = new StockRepository();
    private final QuantityStockStore<Card> stock = new QuantityStockStore<>(
            "cards.json", CardDataService.getInstance().getCards(), repository);
    private final SkillStockStore skills = new SkillStockStore("cards.json", repository);

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

    public boolean isSkill(Card card) {
        return SkillAspectResolver.isSkill(card);
    }

    public SkillConfiguration getSkillConfiguration(String id) {
        return skills.configuration(id);
    }

    public Map<String, Integer> getEffectiveAspects(Card card) {
        return SkillAspectResolver.resolve(card, skills.configuration(card.getId()));
    }

    public List<String> getInitialWisdoms(Card card) {
        return SkillAspectResolver.initialWisdoms(card);
    }

    public void setSkillLevel(String id, int level) {
        SkillConfiguration current = skills.configuration(id);
        skills.setConfiguration(id, new SkillConfiguration(
                level, current.wisdomId(), current.attunementId(), current.harmonized()));
    }

    public void presentSkill(Card card, String wisdomId, String attunementId) {
        if (!getInitialWisdoms(card).contains(wisdomId)) {
            throw new IllegalArgumentException("只能呈递至该 Skill 原有的伟大之术");
        }
        if (!SkillAspectResolver.ATTUNEMENT_IDS.contains(attunementId)) {
            throw new IllegalArgumentException("未知的魂质调和类型");
        }
        SkillConfiguration current = skills.configuration(card.getId());
        skills.setConfiguration(card.getId(), new SkillConfiguration(
                current.level(), wisdomId, attunementId, false));
    }

    public void cancelPresentation(String id) {
        SkillConfiguration current = skills.configuration(id);
        skills.setConfiguration(id, new SkillConfiguration(current.level(), null, null, false));
    }

    public void setHarmonized(String id, boolean harmonized) {
        SkillConfiguration current = skills.configuration(id);
        if (!current.presented() || current.attunementId() == null) {
            throw new IllegalStateException("Skill 尚未呈递和选择调和魂质");
        }
        skills.setConfiguration(id, new SkillConfiguration(
                current.level(), current.wisdomId(), current.attunementId(), harmonized));
    }
}
