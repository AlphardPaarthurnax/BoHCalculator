package com.github.alphardpaarthurnax.bohcalculator.craft;

import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadingRecipeFactoryTest {
    @Test
    void createsARepeatableBookAndSoulRecipeForOrdinaryBooks() {
        Book book = book("t.theopenhead", "mem.revelation", "readable");
        Workstation desk = workstation("library.desk.eva.consider");
        Workstation projector = workstation("library.projector.consider");

        Recipe recipe = new ReadingRecipeFactory()
                .create(List.of(book), List.of(desk, projector))
                .getFirst();

        assertEquals("read.mastered.t.theopenhead", recipe.getId());
        LinkedHashMap<String, Integer> requirements = new LinkedHashMap<>();
        requirements.put("t.theopenhead", 1);
        requirements.put("ability", 1);
        assertEquals(requirements, recipe.getRequirements());
        assertEquals(java.util.Map.of("mem.revelation", 1), recipe.getEffects());
        assertEquals(List.of("library.desk.eva.consider"), recipe.getVerbIds());
    }

    @Test
    void routesFilmsAndRecordingsToTheirSpecialReaders() {
        Book film = book("t.film", "mem.sight", "readable", "film");
        Book record = book("t.record", "mem.sound", "readable", "record.phonograph");
        List<Workstation> stations = List.of(
                workstation("library.desk.eva.consider"),
                workstation("library.projector.consider"),
                workstation("library.phonograph.consider"));

        List<Recipe> recipes = new ReadingRecipeFactory().create(List.of(film, record), stations);

        assertEquals(List.of("library.projector.consider"), recipes.get(0).getVerbIds());
        assertEquals(List.of("library.phonograph.consider"), recipes.get(1).getVerbIds());
    }

    private Book book(String id, String memoryId, String... aspects) {
        Book book = new Book();
        book.setId(id);
        book.setReadingMemoryId(memoryId);
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        for (String aspect : aspects) {
            values.put(aspect, 1);
        }
        book.setAspects(values);
        return book;
    }

    private Workstation workstation(String id) {
        Workstation workstation = new Workstation();
        workstation.setId(id);
        return workstation;
    }
}
