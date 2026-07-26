package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import com.github.alphardpaarthurnax.bohcalculator.service.BookDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CardBrowserSupport;
import javafx.collections.ObservableList;

public final class BookBrowserController extends CardBrowserSupport<Book> {
    @Override
    protected ObservableList<Book> sourceCards() {
        return BookDataService.getInstance().getBooks();
    }
}
