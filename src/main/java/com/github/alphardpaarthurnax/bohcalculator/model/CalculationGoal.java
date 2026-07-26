package com.github.alphardpaarthurnax.bohcalculator.model;

public record CalculationGoal(CalculationGoalType type, String targetId, int amount) {
    public CalculationGoal {
        if (type == null) {
            throw new IllegalArgumentException("需求类型不能为空");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("需求 ID 不能为空");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("需求数量至少为 1");
        }
    }
}
