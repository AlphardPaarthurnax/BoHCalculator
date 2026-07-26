package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import com.github.alphardpaarthurnax.bohcalculator.service.BookStockService;
import com.github.alphardpaarthurnax.bohcalculator.utils.UnlockStockSupport;
import javafx.collections.ObservableList;

public final class BookStockController extends UnlockStockSupport<Book> {
    private final BookStockService service = BookStockService.getInstance();

    @Override protected ObservableList<Book> sourceItems() { return service.getBooks(); }
    @Override protected boolean isUnlocked(String id) { return service.isUnlocked(id); }
    @Override protected void setUnlocked(String id, boolean value) { service.setUnlocked(id, value); }
}
