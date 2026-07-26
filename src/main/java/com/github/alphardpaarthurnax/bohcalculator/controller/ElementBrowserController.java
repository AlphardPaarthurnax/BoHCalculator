package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import com.github.alphardpaarthurnax.bohcalculator.service.ElementDataService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogBrowserSupport;
import javafx.collections.ObservableList;

import java.util.List;

public final class ElementBrowserController extends CatalogBrowserSupport<Element> {
    @Override protected String title() { return "Elements：Rowenarium 抓取的全部 Element（不索引图片）"; }

    @Override protected boolean displayImages() { return false; }

    @Override protected ObservableList<Element> sourceItems() {
        return ElementDataService.getInstance().getElements();
    }

    @Override protected List<DetailRow> details(Element item) {
        return List.of(
                row("分类", item.isAspect() ? "Aspect" : "Card"),
                row("Aspects", formatMap(item.getAspects())),
                row("唯一", item.isUnique() ? "是" : "否"),
                row("Manifestation", item.getManifestationType()),
                row("网页图片名", imageFileName(item)),
                row("网页图片路径", item.getRowenariumImageSrc() != null
                        ? item.getRowenariumImageSrc() : "网页无图片")
        );
    }
}
