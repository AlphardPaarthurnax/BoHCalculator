package com.github.alphardpaarthurnax.bohcalculator.model;

public record CraftPlanMissing(
        CraftPlanMissingType type,
        String targetId,
        int required,
        int available,
        String detail
) {
}
