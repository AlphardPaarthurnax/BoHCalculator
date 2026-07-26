package com.github.alphardpaarthurnax.bohcalculator.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

abstract class SdeDataService<T extends CatalogItem> {
    private static final Path SOURCE_SDE_DIR = Path.of("src/main/resources/assets/sde");

    private final String fileName;
    private final String rootKey;
    private final Class<T> valueType;
    private final Predicate<T> filter;
    private final ObservableList<T> cache = FXCollections.observableArrayList();
    private final ObjectMapper mapper = new ObjectMapper();

    protected SdeDataService(String fileName, String rootKey, Class<T> valueType, Predicate<T> filter) {
        this.fileName = fileName;
        this.rootKey = rootKey;
        this.valueType = valueType;
        this.filter = filter;
        reload();
    }

    public ObservableList<T> getItems() {
        return cache;
    }

    public final void reload() {
        cache.setAll(load());
    }

    private List<T> load() {
        try (InputStream input = openSde()) {
            if (input == null) {
                return List.of();
            }
            JsonNode root = mapper.readTree(input);
            JsonNode values = root.path(rootKey);
            JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, valueType);
            List<T> loaded = mapper.convertValue(values, listType);
            List<T> visible = new ArrayList<>();
            for (T item : loaded) {
                if (!filter.test(item)) {
                    continue;
                }
                visible.add(item);
            }
            return visible;
        } catch (Exception exception) {
            System.err.println("Failed to load " + fileName + ": " + exception.getMessage());
            return List.of();
        }
    }

    private InputStream openSde() throws IOException {
        Path source = SOURCE_SDE_DIR.resolve(fileName);
        if (Files.isRegularFile(source)) {
            return Files.newInputStream(source);
        }
        return getClass().getResourceAsStream("/assets/sde/" + fileName);
    }

}
