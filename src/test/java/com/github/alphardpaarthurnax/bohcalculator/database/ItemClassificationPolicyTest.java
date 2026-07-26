package com.github.alphardpaarthurnax.bohcalculator.database;

import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemClassificationPolicyTest {
    @Test
    void appliesReadableThenWallartThenComfortThenThingPriority() {
        assertEquals(ItemClassificationPolicy.Kind.BOOK,
                ItemClassificationPolicy.classify(element("thing", "comfort", "wallart", "readable")));
        assertEquals(ItemClassificationPolicy.Kind.WALLART,
                ItemClassificationPolicy.classify(element("thing", "comfort", "wallart")));
        assertEquals(ItemClassificationPolicy.Kind.COMFORT,
                ItemClassificationPolicy.classify(element("thing", "comfort")));
        assertEquals(ItemClassificationPolicy.Kind.THING,
                ItemClassificationPolicy.classify(element("thing")));
        assertEquals(ItemClassificationPolicy.Kind.NONE,
                ItemClassificationPolicy.classify(element("memory", "edge")));
    }

    private Element element(String... aspects) {
        Element element = new Element();
        for (String aspect : aspects) {
            element.getAspects().put(aspect, 1);
        }
        return element;
    }
}
