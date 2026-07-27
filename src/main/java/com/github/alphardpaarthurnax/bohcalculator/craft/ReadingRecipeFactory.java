package com.github.alphardpaarthurnax.bohcalculator.craft;

import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/** Builds repeat-reading recipes from the XTrigger metadata on mastered books. */
public final class ReadingRecipeFactory {
    private static final String RECIPE_PREFIX = "read.mastered.";

    public List<Recipe> create(Collection<Book> books, Collection<Workstation> workstations) {
        List<Recipe> result = new ArrayList<>();
        for (Book book : books) {
            if (book.getReadingMemoryId() == null || book.getReadingMemoryId().isBlank()) {
                continue;
            }
            List<String> stations = compatibleWorkstations(book, workstations);
            if (stations.isEmpty()) {
                continue;
            }
            Recipe recipe = new Recipe();
            recipe.setId(RECIPE_PREFIX + book.getId());
            recipe.setLabel("重读：" + book.getDisplayName());
            recipe.setDesc("重读已精通的读物，唤起固定的回忆。");
            recipe.setStartDescription("已精通的读物无需再次满足奥秘，只需投入一张可用魂质。");
            recipe.setCraftable(true);
            recipe.setVerbIds(stations);
            LinkedHashMap<String, Integer> requirements = new LinkedHashMap<>();
            requirements.put(book.getId(), 1);
            requirements.put("ability", 1);
            recipe.setRequirements(requirements);
            LinkedHashMap<String, Integer> effects = new LinkedHashMap<>();
            effects.put(book.getReadingMemoryId(), 1);
            recipe.setEffects(effects);
            result.add(recipe);
        }
        return List.copyOf(result);
    }

    private List<String> compatibleWorkstations(Book book, Collection<Workstation> workstations) {
        boolean film = book.getAspects().getOrDefault("film", 0) > 0;
        boolean recording = book.getAspects().getOrDefault("record.phonograph", 0) > 0;
        return workstations.stream()
                .map(Workstation::getId)
                .filter(id -> film ? id.contains("projector")
                        : recording ? id.contains("phonograph")
                        : id.startsWith("library.desk.") && id.endsWith(".consider"))
                .toList();
    }
}
