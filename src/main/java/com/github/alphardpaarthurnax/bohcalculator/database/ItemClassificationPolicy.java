package com.github.alphardpaarthurnax.bohcalculator.database;

import com.github.alphardpaarthurnax.bohcalculator.model.Element;

public final class ItemClassificationPolicy {
    private ItemClassificationPolicy() {
    }

    public static Kind classify(Element element) {
        if (element.getAspects().containsKey("readable")) {
            return Kind.BOOK;
        }
        if (element.getAspects().containsKey("wallart")) {
            return Kind.WALLART;
        }
        if (element.getAspects().containsKey("comfort")) {
            return Kind.COMFORT;
        }
        if (element.getAspects().containsKey("thing")) {
            return Kind.THING;
        }
        return Kind.NONE;
    }

    public enum Kind {
        NONE,
        BOOK,
        WALLART,
        COMFORT,
        THING
    }
}
