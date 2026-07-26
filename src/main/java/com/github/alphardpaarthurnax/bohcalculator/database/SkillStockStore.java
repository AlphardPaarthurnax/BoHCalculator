package com.github.alphardpaarthurnax.bohcalculator.database;

import com.github.alphardpaarthurnax.bohcalculator.model.SkillConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SkillStockStore {
    private final String fileName;
    private final StockRepository repository;
    private final Map<String, SkillConfiguration> configurations;

    public SkillStockStore(String fileName, StockRepository repository) {
        this.fileName = fileName;
        this.repository = repository;
        configurations = new LinkedHashMap<>(repository.loadSkillConfigurations(fileName));
    }

    public SkillConfiguration configuration(String skillId) {
        return configurations.getOrDefault(skillId, SkillConfiguration.DEFAULT);
    }

    public synchronized void setConfiguration(String skillId, SkillConfiguration configuration) {
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("Skill ID 不能为空");
        }
        Map<String, SkillConfiguration> updated = new LinkedHashMap<>(configurations);
        if (configuration == null || configuration.isDefault()) {
            updated.remove(skillId);
        } else {
            updated.put(skillId, configuration);
        }
        repository.saveSkillConfigurations(fileName, updated);
        configurations.clear();
        configurations.putAll(updated);
    }
}
