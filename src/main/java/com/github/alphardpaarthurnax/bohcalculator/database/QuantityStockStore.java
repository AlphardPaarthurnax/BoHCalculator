package com.github.alphardpaarthurnax.bohcalculator.database;

import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.collections.ObservableList;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QuantityStockStore<T extends CatalogItem> {
    private final String fileName;
    private final ObservableList<T> catalog;
    private final StockRepository repository;
    private final Map<String, Integer> quantities;

    public QuantityStockStore(String fileName, ObservableList<T> catalog) {
        this(fileName, catalog, new StockRepository());
    }

    QuantityStockStore(String fileName, ObservableList<T> catalog, StockRepository repository) {
        this.fileName = fileName;
        this.catalog = catalog;
        this.repository = repository;
        quantities = new LinkedHashMap<>(repository.loadQuantities(fileName));
    }

    public ObservableList<T> catalog() {
        return catalog;
    }

    public int quantity(String id) {
        return quantities.getOrDefault(id, 0);
    }

    public synchronized void setQuantity(String id, int amount) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("库存 ID 不能为空");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("库存数量不能为负数");
        }
        Map<String, Integer> updated = new LinkedHashMap<>(quantities);
        if (amount == 0) {
            updated.remove(id);
        } else {
            updated.put(id, amount);
        }
        repository.saveQuantities(fileName, updated);
        quantities.clear();
        quantities.putAll(updated);
    }
}
