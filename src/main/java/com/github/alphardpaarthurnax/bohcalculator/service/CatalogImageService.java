package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class CatalogImageService {
    private static final Map<String, Image> CACHE = new HashMap<>();

    private CatalogImageService() {
    }

    public static Image imageFor(CatalogItem item) {
        if (item == null || item.getImagePath() == null || item.getImagePath().isBlank()) {
            return null;
        }
        Image cached = CACHE.get(item.getImagePath());
        if (cached != null) {
            item.setImage(cached);
            return cached;
        }
        Image loaded = load(item.getImagePath());
        if (loaded != null) {
            CACHE.put(item.getImagePath(), loaded);
            item.setImage(loaded);
        }
        return loaded;
    }

    private static Image load(String resourcePath) {
        try (InputStream input = CatalogImageService.class.getResourceAsStream(resourcePath)) {
            if (input != null) {
                return new Image(input, 96, 96, true, true);
            }
        } catch (IOException ignored) {
        }
        Path source = Path.of("src/main/resources" + resourcePath.replace('/', java.io.File.separatorChar));
        if (Files.isRegularFile(source)) {
            try (InputStream input = Files.newInputStream(source)) {
                return new Image(input, 96, 96, true, true);
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
