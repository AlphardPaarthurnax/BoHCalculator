package com.github.alphardpaarthurnax.bohcalculator.service;

import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowenariumParserTest {
    private final RowenariumParser parser = new RowenariumParser();

    @Test
    void parsesIndexesAndChineseElementData() {
        Document document = page("""
                <a class="section-item" href="/rowenarium/element/with%%20space">with space</a>
                <a class="section-item" href="/rowenarium/element/zeta">zeta</a>
                <a class="section-item" href="/rowenarium/element/edge">edge</a>
                <a class="section-item" href="/rowenarium/recipe/craft.edge">craft.edge</a>
                <div class="section-file"><div class="section-file-title">_aspects.json</div>
                  <a id="section-item-active">edge</a></div>
                <div id="data-page">
                  <img class="content-image" src="/rowenarium/static/bhimages/aspects/edge.png">
                  %s
                  <p class="content-field"><strong class="field-title">Aspects:</strong>
                    <a href="/rowenarium/element/principle"><span class="ref-amount">2</span><span class="ref-id">principle</span></a></p>
                  <p class="content-field"><strong class="field-title">Hidden?</strong>No</p>
                  <p class="content-field"><strong class="field-title">Unique?</strong>Yes</p>
                </div>
                """.formatted(localizedFields("Edge", "Грань", "刃", "Sharp", "Остро", "锋锐")));

        assertEquals(List.of("edge", "with space", "zeta"), parser.parseIndex(document, "element"));
        assertEquals(List.of("craft.edge"), parser.parseIndex(document, "recipe"));

        Element element = parser.parseElement(document, "edge");
        assertEquals("刃", element.getLabel());
        assertEquals("锋锐", element.getDesc());
        assertEquals(2, element.getAspects().get("principle"));
        assertEquals("_aspects.json", element.getSourceFile());
        assertFalse(element.isAspect());
        assertTrue(element.isUnique());
        assertFalse(element.isHidden());
    }

    @Test
    void parsesRecipeRequirementsEffectsAndVerbs() {
        Document document = page("""
                <meta name="twitter:image" content="/rowenarium/static/bhimages/elements/result.png">
                <div id="data-page">
                  %s
                  <p class="content-field"><strong class="field-title">Start Description:</strong>
                    <ul class="localizations"><li class="localizations-item">Start</li><li class="localizations-item">Начало</li><li class="localizations-item">开始</li></ul></p>
                  <p class="content-field"><strong class="field-title">Verb:</strong>
                    <a href="/rowenarium/verb/library.desk"><span class="ref-id">library.desk</span></a></p>
                  <p class="content-field"><strong class="field-title">Requirements:</strong>
                    <a href="/rowenarium/element/edge"><span class="ref-amount">5</span><span class="ref-id">edge</span></a></p>
                  <p class="content-field"><strong class="field-title">Effects:</strong>
                    <a href="/rowenarium/element/result"><span class="ref-id">result</span></a></p>
                  <p class="content-field"><strong class="field-title">Craftable?</strong>Yes</p>
                </div>
                """.formatted(localizedFields("Recipe", "Рецепт", "配方", "Done", "Готово", "完成")));

        Recipe recipe = parser.parseRecipe(document, "craft.edge");
        assertEquals("配方", recipe.getLabel());
        assertEquals("开始", recipe.getStartDescription());
        assertEquals(List.of("library.desk"), recipe.getVerbIds());
        assertEquals(5, recipe.getRequirements().get("edge"));
        assertEquals(1, recipe.getEffects().get("result"));
        assertTrue(recipe.isCraftable());
        assertEquals(null, recipe.getRowenariumImageSrc());
    }

    @Test
    void parsesVerbSlotsWithoutGuessingTheirMeaning() {
        Document document = page("""
                <div id="data-page">
                  <img class="content-image" src="/rowenarium/static/bhimages/verbs/library.desk.png">
                  %s
                  <div class="content-field"><strong class="field-title">Slots:</strong><ul><li>
                    <span class="content-subfield"><strong class="subfield-title">Label:</strong>
                      <ul><li class="localizations-item">Soul</li><li class="localizations-item">Душа</li><li class="localizations-item">魂质</li></ul></span>
                    <span class="content-subfield"><strong class="subfield-title">Essential:</strong>
                      <a href="/rowenarium/element/ability"><span class="ref-id">ability</span></a></span>
                    <span class="content-subfield"><strong class="subfield-title">Required:</strong>
                      <a href="/rowenarium/element/fet"><span class="ref-id">fet</span></a></span>
                    <span class="content-subfield"><strong class="subfield-title">Forbidden:</strong>
                      <a href="/rowenarium/element/fatigued"><span class="ref-id">fatigued</span></a></span>
                    <span class="content-subfield"><strong class="subfield-title">Consumes?</strong>No</span>
                  </li></ul></div>
                </div>
                """.formatted(localizedFields("Desk", "Стол", "书桌", "Work", "Работа", "工作")));

        Verb verb = parser.parseVerb(document, "library.desk");
        assertEquals(1, verb.getSlots().size());
        assertEquals("魂质", verb.getSlots().getFirst().getLabel());
        assertEquals(List.of("ability"), verb.getSlots().getFirst().getEssential());
        assertEquals(List.of("fet"), verb.getSlots().getFirst().getRequired());
        assertEquals(List.of("fatigued"), verb.getSlots().getFirst().getForbidden());
        assertFalse(verb.getSlots().getFirst().isConsumes());
    }

    private Document page(String body) {
        return Jsoup.parse("<html><head></head><body>" + body + "</body></html>");
    }

    private String localizedFields(String enLabel, String ruLabel, String zhLabel,
                                   String enDescription, String ruDescription, String zhDescription) {
        return """
                <p class="content-field"><strong class="field-title">Label:</strong>
                  <ul class="localizations"><li class="localizations-item">%s</li><li class="localizations-item">%s</li><li class="localizations-item">%s</li></ul></p>
                <p class="content-field"><strong class="field-title">Description:</strong>
                  <ul class="localizations"><li class="localizations-item">%s</li><li class="localizations-item">%s</li><li class="localizations-item">%s</li></ul></p>
                """.formatted(enLabel, ruLabel, zhLabel, enDescription, ruDescription, zhDescription);
    }
}
