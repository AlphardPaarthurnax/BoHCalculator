package com.github.alphardpaarthurnax.bohcalculator.model;

public record SkillConfiguration(
        int level,
        String wisdomId,
        String attunementId,
        boolean harmonized
) {
    public static final SkillConfiguration DEFAULT = new SkillConfiguration(1, null, null, false);

    public SkillConfiguration {
        if (level < 1 || level > 9) {
            throw new IllegalArgumentException("Skill 等级必须在 1 到 9 之间");
        }
        wisdomId = blankToNull(wisdomId);
        attunementId = blankToNull(attunementId);
        if (wisdomId == null) {
            attunementId = null;
            harmonized = false;
        }
        if (attunementId == null) {
            harmonized = false;
        }
    }

    public boolean presented() {
        return wisdomId != null;
    }

    public boolean isDefault() {
        return level == 1 && !presented();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
