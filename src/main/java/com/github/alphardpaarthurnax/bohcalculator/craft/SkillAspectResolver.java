package com.github.alphardpaarthurnax.bohcalculator.craft;

import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.SkillConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies the player's level, wisdom presentation and soul attunement to a Skill. */
public final class SkillAspectResolver {
    public static final Set<String> BASIC_ASPECT_IDS = Set.of(
            "knock", "lantern", "forge", "edge", "winter", "heart", "grail",
            "moth", "nectar", "scale", "sky", "rose", "moon");
    public static final List<String> WISDOM_IDS = List.of(
            "w.horomachistry", "w.ithastry", "w.illumination", "w.hushery",
            "w.nyctodromy", "w.skolekosophy", "w.bosk", "w.preservation", "w.birdsong");
    public static final List<String> ATTUNEMENT_IDS = List.of(
            "a.xhea", "a.xpho", "a.xmet", "a.xfet", "a.xsha",
            "a.xcho", "a.xere", "a.xtri", "a.xwis");
    public static final String SKILL_ASPECT_ID = "skill";
    public static final String PRESENTED_ASPECT_ID = "wisdom.committed";
    public static final String HARMONIZED_ASPECT_ID = "a.xhausted";

    private SkillAspectResolver() {
    }

    public static boolean isSkill(Card card) {
        return card != null && card.getAspects().getOrDefault(SKILL_ASPECT_ID, 0) > 0;
    }

    public static List<String> initialWisdoms(Card card) {
        if (card == null) {
            return List.of();
        }
        return WISDOM_IDS.stream()
                .filter(id -> card.getAspects().getOrDefault(id, 0) > 0)
                .toList();
    }

    public static Map<String, Integer> resolve(Card card, SkillConfiguration configuration) {
        Map<String, Integer> result = new LinkedHashMap<>(card.getAspects());
        if (!isSkill(card)) {
            return result;
        }

        int increase = configuration.level() - 1;
        if (increase > 0) {
            BASIC_ASPECT_IDS.forEach(id -> {
                Integer base = result.get(id);
                if (base != null && base > 0) {
                    result.put(id, base + increase);
                }
            });
            result.computeIfPresent(SKILL_ASPECT_ID, (id, base) -> base + increase);
        }

        if (configuration.presented()) {
            int selectedAmount = Math.max(1, card.getAspects().getOrDefault(configuration.wisdomId(), 1));
            WISDOM_IDS.forEach(result::remove);
            result.put(configuration.wisdomId(), selectedAmount);
            result.put(PRESENTED_ASPECT_ID, 1);
            ATTUNEMENT_IDS.forEach(result::remove);
            result.remove(HARMONIZED_ASPECT_ID);
            if (configuration.harmonized()) {
                result.put(HARMONIZED_ASPECT_ID, 1);
            } else if (configuration.attunementId() != null) {
                result.put(configuration.attunementId(), 1);
            }
        }
        return result;
    }
}
