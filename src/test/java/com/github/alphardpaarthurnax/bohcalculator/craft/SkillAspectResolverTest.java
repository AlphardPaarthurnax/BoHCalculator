package com.github.alphardpaarthurnax.bohcalculator.craft;

import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.SkillConfiguration;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SkillAspectResolverTest {
    @Test
    void levelOnlyRaisesOriginalPrinciplesAndSkill() {
        Card skill = skill();

        Map<String, Integer> resolved = SkillAspectResolver.resolve(
                skill, new SkillConfiguration(3, null, null, false));

        assertEquals(4, resolved.get("lantern"));
        assertEquals(3, resolved.get("sky"));
        assertEquals(3, resolved.get("skill"));
        assertFalse(resolved.containsKey("edge"));
        assertEquals(1, resolved.get("w.illumination"));
        assertEquals(1, resolved.get("w.hushery"));
    }

    @Test
    void presentationKeepsChosenWisdomAndAddsAttunement() {
        Map<String, Integer> resolved = SkillAspectResolver.resolve(
                skill(), new SkillConfiguration(2, "w.illumination", "a.xpho", false));

        assertEquals(1, resolved.get("w.illumination"));
        assertFalse(resolved.containsKey("w.hushery"));
        assertEquals(1, resolved.get("wisdom.committed"));
        assertEquals(1, resolved.get("a.xpho"));
        assertFalse(resolved.containsKey("a.xhausted"));
    }

    @Test
    void harmonizationReplacesAttunementWithExhaustedMarker() {
        Map<String, Integer> resolved = SkillAspectResolver.resolve(
                skill(), new SkillConfiguration(2, "w.illumination", "a.xpho", true));

        assertFalse(resolved.containsKey("a.xpho"));
        assertEquals(1, resolved.get("a.xhausted"));
    }

    private Card skill() {
        Card card = new Card();
        card.setId("s.test");
        card.setAspects(new LinkedHashMap<>(Map.of(
                "lantern", 2,
                "sky", 1,
                "w.illumination", 1,
                "w.hushery", 1,
                "skill", 1)));
        return card;
    }
}
