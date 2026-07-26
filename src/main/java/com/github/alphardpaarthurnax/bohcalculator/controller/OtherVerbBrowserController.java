package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import com.github.alphardpaarthurnax.bohcalculator.service.OtherVerbDataService;
import javafx.collections.ObservableList;

public class OtherVerbBrowserController extends VerbBrowserController {
    @Override protected String title() { return "OtherVerbs：未被任何可合成 Craft 引用的 Verb（不索引图片）"; }
    @Override protected ObservableList<Verb> sourceItems() { return OtherVerbDataService.getInstance().getOtherVerbs(); }
}
