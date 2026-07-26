package com.github.alphardpaarthurnax.bohcalculator.database;

import com.github.alphardpaarthurnax.bohcalculator.model.Element;

public final class ElementClassificationPolicy {
    private ElementClassificationPolicy() {
    }

    public static boolean hasNormalImage(String imageSrc) {
        if (imageSrc == null || imageSrc.isBlank()) {
            return false;
        }
        String normalized = imageSrc.replace('\\', '/').toLowerCase();
        return normalized.contains("/rowenarium/static/bhimages/") && !normalized.endsWith("/_x.png");
    }

    public static void classify(Element element) {
        element.setAspect(element.getAspects().isEmpty());
        element.setNormalImage(hasNormalImage(element.getRowenariumImageSrc()));
    }

    public static boolean isVisible(Element element) {
        return element.isNormalImage();
    }

    public static boolean isCard(Element element) {
        return !element.isAspect();
    }
}
