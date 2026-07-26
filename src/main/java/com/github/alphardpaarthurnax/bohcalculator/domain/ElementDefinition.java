package com.github.alphardpaarthurnax.bohcalculator.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Solver-facing definition of a Rowenarium element.
 *
 * <p>The element is retained even when it is hidden or its local image is
 * missing. Those properties only control whether it is shown in a browser.</p>
 */
public record ElementDefinition(
        String id,
        Map<String, Integer> aspects,
        boolean hidden,
        String rowenariumImageSrc
) {
    public ElementDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Map<String, Integer> normalized = new LinkedHashMap<>();
        if (aspects != null) {
            aspects.forEach((aspectId, amount) -> {
                if (aspectId == null || aspectId.isBlank()) {
                    throw new IllegalArgumentException("aspect id must not be blank");
                }
                if (amount == null) {
                    throw new IllegalArgumentException("aspect amount must not be null");
                }
                normalized.put(aspectId, amount);
            });
        }
        aspects = Collections.unmodifiableMap(normalized);
    }

    public int aspectAmount(String aspectId) {
        return aspects.getOrDefault(aspectId, 0);
    }

    public boolean hasAspect(String aspectId) {
        return aspectAmount(aspectId) > 0;
    }

    public boolean isMemory() {
        return hasAspect("memory");
    }
}
