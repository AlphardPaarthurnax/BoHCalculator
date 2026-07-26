package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import com.github.alphardpaarthurnax.bohcalculator.model.VerbSlot;
import com.github.alphardpaarthurnax.bohcalculator.service.VerbDataService;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class VerbBrowserController extends CatalogBrowserController<Verb> {
    @Override protected String title() { return "Verbs：Rowenarium 抓取的全部 Verb（不索引图片）"; }

    @Override protected boolean displayImages() { return false; }

    @Override protected ObservableList<Verb> sourceItems() {
        return VerbDataService.getInstance().getVerbs();
    }

    @Override protected List<DetailRow> details(Verb item) {
        List<DetailRow> rows = new ArrayList<>();
        rows.add(row("Aspects", formatMap(item.getAspects())));
        rows.add(row("槽位数", item.getSlots().size()));
        rows.add(row("网页图片名", imageFileName(item)));
        rows.add(row("网页图片路径", item.getRowenariumImageSrc() != null
                ? item.getRowenariumImageSrc() : "网页无图片"));
        for (int index = 0; index < item.getSlots().size(); index++) {
            rows.add(row("槽位 " + (index + 1), formatSlot(item.getSlots().get(index))));
        }
        return rows;
    }

    protected String formatSlot(VerbSlot slot) {
        return (slot.getLabel() != null ? slot.getLabel() : "未命名")
                + "\n必须包含：" + String.join(", ", slot.getEssential())
                + "\n允许：" + String.join(", ", slot.getRequired())
                + "\n禁止：" + String.join(", ", slot.getForbidden())
                + "\n消耗：" + (slot.isConsumes() ? "是" : "否");
    }
}
