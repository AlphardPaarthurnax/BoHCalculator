package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.database.ElementClassificationPolicy;
import com.github.alphardpaarthurnax.bohcalculator.database.SdeDataService;
import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import javafx.collections.ObservableList;

public final class BookDataService extends SdeDataService<Book> {
    private static final BookDataService INSTANCE = new BookDataService();

    private BookDataService() {
        super("books.json", "books", Book.class,
                item -> ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
    }

    public static BookDataService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Book> getBooks() {
        return getItems();
    }
}
