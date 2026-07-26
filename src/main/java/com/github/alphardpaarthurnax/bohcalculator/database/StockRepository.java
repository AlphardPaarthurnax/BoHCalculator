package com.github.alphardpaarthurnax.bohcalculator.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class StockRepository {
    public static final Path DEFAULT_STOCK_DIR = Path.of("src/main/resources/assets/sde/stock");

    private final Path stockDirectory;
    private final boolean loadBundledDefaults;
    private final ObjectMapper mapper = new ObjectMapper();

    public StockRepository() {
        this(DEFAULT_STOCK_DIR, true);
    }

    public StockRepository(Path stockDirectory) {
        this(stockDirectory, false);
    }

    private StockRepository(Path stockDirectory, boolean loadBundledDefaults) {
        this.stockDirectory = stockDirectory;
        this.loadBundledDefaults = loadBundledDefaults;
    }

    public Map<String, Integer> loadQuantities(String fileName) {
        JsonNode quantities = load(fileName).path("quantities");
        Map<String, Integer> result = new LinkedHashMap<>();
        if (quantities.isObject()) {
            quantities.fields().forEachRemaining(entry -> {
                int amount = entry.getValue().asInt(0);
                if (amount > 0) {
                    result.put(entry.getKey(), amount);
                }
            });
        }
        return result;
    }

    public Set<String> loadUnlocked(String fileName) {
        JsonNode unlocked = load(fileName).path("unlocked");
        Set<String> result = new LinkedHashSet<>();
        if (unlocked.isArray()) {
            unlocked.forEach(value -> {
                String id = value.asText("");
                if (!id.isBlank()) {
                    result.add(id);
                }
            });
        }
        return result;
    }

    public void saveQuantities(String fileName, Map<String, Integer> quantities) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode values = root.putObject("quantities");
        quantities.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        save(fileName, root);
    }

    public void saveUnlocked(String fileName, Set<String> unlocked) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode values = root.putArray("unlocked");
        unlocked.stream()
                .filter(id -> id != null && !id.isBlank())
                .sorted(Comparator.naturalOrder())
                .forEach(values::add);
        save(fileName, root);
    }

    private JsonNode load(String fileName) {
        Path source = stockDirectory.resolve(fileName);
        try {
            if (Files.isRegularFile(source)) {
                return mapper.readTree(source.toFile());
            }
            if (loadBundledDefaults) {
                try (InputStream input = getClass().getResourceAsStream("/assets/sde/stock/" + fileName)) {
                    return input != null ? mapper.readTree(input) : mapper.createObjectNode();
                }
            }
            return mapper.createObjectNode();
        } catch (IOException exception) {
            throw new UncheckedIOException("无法读取库存文件：" + source, exception);
        }
    }

    private void save(String fileName, JsonNode root) {
        Path target = stockDirectory.resolve(fileName);
        Path temporary = null;
        try {
            Files.createDirectories(stockDirectory);
            temporary = Files.createTempFile(stockDirectory, fileName + ".", ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), root);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("无法保存库存文件：" + target, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The completed move normally leaves no temporary file.
                }
            }
        }
    }
}
