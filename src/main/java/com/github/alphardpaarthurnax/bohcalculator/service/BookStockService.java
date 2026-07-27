package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.UnlockStockStore;
import com.github.alphardpaarthurnax.bohcalculator.database.StockRepository;
import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import javafx.collections.ObservableList;

import java.util.LinkedHashSet;
import java.util.Set;

public final class BookStockService {
    private static final BookStockService INSTANCE = new BookStockService();
    private final UnlockStockStore<Book> stock = new UnlockStockStore<>(
            "books.json", BookDataService.getInstance().getBooks());
    private final StockRepository repository = new StockRepository();
    private final Set<String> mastered = new LinkedHashSet<>(repository.loadIds("books.json", "mastered"));

    private BookStockService() {
    }

    public static BookStockService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Book> getBooks() {
        return stock.catalog();
    }

    public boolean isUnlocked(String id) {
        return stock.isUnlocked(id);
    }

    public void setUnlocked(String id, boolean unlocked) {
        if (!unlocked && mastered.contains(id)) {
            setMastered(id, false);
        }
        stock.setUnlocked(id, unlocked);
    }

    public boolean isMastered(String id) {
        return isUnlocked(id) && mastered.contains(id);
    }

    public synchronized void setMastered(String id, boolean value) {
        if (value && !isUnlocked(id)) {
            throw new IllegalArgumentException("只有已解锁的书籍才能标记为已精通");
        }
        Set<String> updated = new LinkedHashSet<>(mastered);
        if (value) {
            updated.add(id);
        } else {
            updated.remove(id);
        }
        repository.saveIds("books.json", "mastered", updated);
        mastered.clear();
        mastered.addAll(updated);
    }
}
