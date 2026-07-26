package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementClassificationPolicyTest {
    @Test
    void elementWithoutAspectsIsAnAspect() {
        Element aspect = element("edge", "/rowenarium/static/bhimages/aspects/edge.png", false);
        ElementClassificationPolicy.classify(aspect);

        assertTrue(aspect.isAspect());
        assertTrue(ElementClassificationPolicy.isVisible(aspect));
        assertFalse(ElementClassificationPolicy.isCard(aspect));
    }

    @Test
    void visibleNonAspectElementIsACardEvenWhenItsLocalImageIsMissing() {
        Element card = element("future.card", "/rowenarium/static/bhimages/elements/future.card.png", false);
        card.getAspects().put("edge", 1);
        ElementClassificationPolicy.classify(card);

        assertFalse(card.isAspect());
        assertTrue(ElementClassificationPolicy.isCard(card));
    }

    @Test
    void imageVisibilityDoesNotChangeAspectOrCardClassification() {
        Element hidden = element("hidden.card", "/rowenarium/static/bhimages/elements/hidden.card.png", true);
        hidden.getAspects().put("edge", 1);
        Element placeholder = element("internal", "/rowenarium/static/bhimages/elements/_x.png", false);
        placeholder.getAspects().put("edge", 1);
        ElementClassificationPolicy.classify(hidden);
        ElementClassificationPolicy.classify(placeholder);

        assertTrue(ElementClassificationPolicy.isCard(hidden));
        assertTrue(ElementClassificationPolicy.isCard(placeholder));
        assertTrue(ElementClassificationPolicy.isVisible(hidden));
        assertFalse(ElementClassificationPolicy.isVisible(placeholder));
    }

    private Element element(String id, String image, boolean hidden) {
        Element result = new Element();
        result.setId(id);
        result.setRowenariumImageSrc(image);
        result.setHidden(hidden);
        return result;
    }
}
