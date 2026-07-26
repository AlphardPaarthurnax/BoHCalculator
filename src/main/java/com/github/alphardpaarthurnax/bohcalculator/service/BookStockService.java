package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.UnlockStockStore;
import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import javafx.collections.ObservableList;

public final class BookStockService {
    private static final BookStockService INSTANCE = new BookStockService();
    private final UnlockStockStore<Book> stock = new UnlockStockStore<>(
            "books.json", BookDataService.getInstance().getBooks());

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
        stock.setUnlocked(id, unlocked);
    }
}
