package com.github.alphardpaarthurnax.bohcalculator.database;

import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.collections.ObservableList;

import java.util.LinkedHashSet;
import java.util.Set;

public final class UnlockStockStore<T extends CatalogItem> {
    private final String fileName;
    private final ObservableList<T> catalog;
    private final StockRepository repository;
    private final Set<String> unlocked;

    public UnlockStockStore(String fileName, ObservableList<T> catalog) {
        this(fileName, catalog, new StockRepository());
    }

    UnlockStockStore(String fileName, ObservableList<T> catalog, StockRepository repository) {
        this.fileName = fileName;
        this.catalog = catalog;
        this.repository = repository;
        unlocked = new LinkedHashSet<>(repository.loadUnlocked(fileName));
    }

    public ObservableList<T> catalog() {
        return catalog;
    }

    public boolean isUnlocked(String id) {
        return unlocked.contains(id);
    }

    public synchronized void setUnlocked(String id, boolean value) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("库存 ID 不能为空");
        }
        Set<String> updated = new LinkedHashSet<>(unlocked);
        if (value) {
            updated.add(id);
        } else {
            updated.remove(id);
        }
        repository.saveUnlocked(fileName, updated);
        unlocked.clear();
        unlocked.addAll(updated);
    }
}
