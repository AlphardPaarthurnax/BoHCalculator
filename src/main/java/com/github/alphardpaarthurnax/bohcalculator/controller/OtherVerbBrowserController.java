package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import com.github.alphardpaarthurnax.bohcalculator.service.OtherVerbDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.VerbBrowserSupport;
import javafx.collections.ObservableList;

public final class OtherVerbBrowserController extends VerbBrowserSupport {
    @Override protected String title() { return "OtherVerbs：未被任何可合成 Craft 引用的 Verb（不索引图片）"; }
    @Override protected ObservableList<Verb> sourceItems() { return OtherVerbDataService.getInstance().getOtherVerbs(); }
}
