package com.github.alphardpaarthurnax.bohcalculator.model;

import java.util.List;
import java.util.Map;

public record SdeGenerationReport(
        String generatedAt,
        Map<String, Integer> counts,
        List<String> failedPages,
        List<String> downloadedImages,
        List<String> missingImages
) {
    public boolean complete() {
        return failedPages.isEmpty();
    }
}
