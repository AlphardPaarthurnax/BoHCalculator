package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import com.github.alphardpaarthurnax.bohcalculator.service.VerbDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.VerbBrowserSupport;
import javafx.collections.ObservableList;

public final class VerbBrowserController extends VerbBrowserSupport {
    @Override
    protected String title() {
        return "Verbs：Rowenarium 抓取的全部 Verb（不索引图片）";
    }

    @Override
    protected ObservableList<Verb> sourceItems() {
        return VerbDataService.getInstance().getVerbs();
    }
}
