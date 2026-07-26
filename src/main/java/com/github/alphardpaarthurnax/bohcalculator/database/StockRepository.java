package com.github.alphardpaarthurnax.bohcalculator.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.alphardpaarthurnax.bohcalculator.model.SkillConfiguration;

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

    public Map<String, SkillConfiguration> loadSkillConfigurations(String fileName) {
        JsonNode skills = load(fileName).path("skills");
        Map<String, SkillConfiguration> result = new LinkedHashMap<>();
        if (skills.isObject()) {
            skills.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                try {
                    SkillConfiguration configuration = new SkillConfiguration(
                            value.path("level").asInt(1),
                            value.path("wisdomId").asText(null),
                            value.path("attunementId").asText(null),
                            value.path("harmonized").asBoolean(false));
                    if (!configuration.isDefault()) {
                        result.put(entry.getKey(), configuration);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid hand-edited entries while retaining the rest of the stock file.
                }
            });
        }
        return result;
    }

    public synchronized void saveQuantities(String fileName, Map<String, Integer> quantities) {
        ObjectNode root = editableRoot(fileName);
        ObjectNode values = mapper.createObjectNode();
        quantities.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        root.set("quantities", values);
        save(fileName, root);
    }

    public synchronized void saveUnlocked(String fileName, Set<String> unlocked) {
        ObjectNode root = editableRoot(fileName);
        ArrayNode values = mapper.createArrayNode();
        unlocked.stream()
                .filter(id -> id != null && !id.isBlank())
                .sorted(Comparator.naturalOrder())
                .forEach(values::add);
        root.set("unlocked", values);
        save(fileName, root);
    }

    public synchronized void saveSkillConfigurations(
            String fileName, Map<String, SkillConfiguration> configurations) {
        ObjectNode root = editableRoot(fileName);
        ObjectNode skills = mapper.createObjectNode();
        configurations.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null && !entry.getValue().isDefault())
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    SkillConfiguration configuration = entry.getValue();
                    ObjectNode value = skills.putObject(entry.getKey());
                    value.put("level", configuration.level());
                    if (configuration.wisdomId() != null) {
                        value.put("wisdomId", configuration.wisdomId());
                    }
                    if (configuration.attunementId() != null) {
                        value.put("attunementId", configuration.attunementId());
                    }
                    value.put("harmonized", configuration.harmonized());
                });
        root.set("skills", skills);
        save(fileName, root);
    }

    private ObjectNode editableRoot(String fileName) {
        JsonNode existing = load(fileName);
        return existing instanceof ObjectNode object ? object.deepCopy() : mapper.createObjectNode();
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
