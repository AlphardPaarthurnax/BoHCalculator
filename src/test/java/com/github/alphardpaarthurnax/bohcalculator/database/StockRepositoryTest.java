package com.github.alphardpaarthurnax.bohcalculator.database;

import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.SkillConfiguration;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsOnlyPositiveQuantitiesAndUnlockedIds() {
        StockRepository repository = new StockRepository(temporaryDirectory);
        Map<String, Integer> quantities = new LinkedHashMap<>();
        quantities.put("card.zero", 0);
        quantities.put("card.two", 2);
        repository.saveQuantities("cards.json", quantities);
        repository.saveUnlocked("books.json",
                new LinkedHashSet<>(Set.of("book.beta", "book.alpha")));

        assertEquals(Map.of("card.two", 2), repository.loadQuantities("cards.json"));
        assertEquals(Set.of("book.alpha", "book.beta"), repository.loadUnlocked("books.json"));
    }

    @Test
    void storesUseDefaultStateAndPersistMutations() {
        StockRepository repository = new StockRepository(temporaryDirectory);
        Card card = new Card();
        card.setId("card.test");

        QuantityStockStore<Card> quantities = new QuantityStockStore<>(
                "cards.json", FXCollections.observableArrayList(card), repository);
        UnlockStockStore<Card> unlocked = new UnlockStockStore<>(
                "books.json", FXCollections.observableArrayList(card), repository);

        assertEquals(0, quantities.quantity(card.getId()));
        assertFalse(unlocked.isUnlocked(card.getId()));

        quantities.setQuantity(card.getId(), 4);
        unlocked.setUnlocked(card.getId(), true);
        assertEquals(4, repository.loadQuantities("cards.json").get(card.getId()));
        assertTrue(repository.loadUnlocked("books.json").contains(card.getId()));

        quantities.setQuantity(card.getId(), 0);
        unlocked.setUnlocked(card.getId(), false);
        assertTrue(repository.loadQuantities("cards.json").isEmpty());
        assertTrue(repository.loadUnlocked("books.json").isEmpty());
    }

    @Test
    void skillConfigurationSurvivesQuantityUpdatesInSameFile() {
        StockRepository repository = new StockRepository(temporaryDirectory);
        SkillConfiguration configuration = new SkillConfiguration(
                4, "w.illumination", "a.xpho", true);

        repository.saveSkillConfigurations("cards.json", Map.of("s.test", configuration));
        repository.saveQuantities("cards.json", Map.of("s.test", 1));

        assertEquals(Map.of("s.test", 1), repository.loadQuantities("cards.json"));
        assertEquals(configuration, repository.loadSkillConfigurations("cards.json").get("s.test"));

        repository.saveSkillConfigurations("cards.json", Map.of());
        assertTrue(repository.loadSkillConfigurations("cards.json").isEmpty());
        assertEquals(Map.of("s.test", 1), repository.loadQuantities("cards.json"));
    }

    @Test
    void masteredBooksSurviveUnlockedUpdatesInSameFile() {
        StockRepository repository = new StockRepository(temporaryDirectory);
        repository.saveIds("books.json", "mastered", Set.of("book.mastered"));
        repository.saveUnlocked("books.json", Set.of("book.mastered", "book.unread"));

        assertEquals(Set.of("book.mastered"), repository.loadIds("books.json", "mastered"));
        assertEquals(Set.of("book.mastered", "book.unread"), repository.loadUnlocked("books.json"));
    }
}
