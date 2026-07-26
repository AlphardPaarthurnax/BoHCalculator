package com.github.alphardpaarthurnax.bohcalculator.database;

import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import com.github.alphardpaarthurnax.bohcalculator.model.VerbSlot;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class RowenariumParser {
    private static final String ROOT = "/rowenarium/";

    public List<String> parseIndex(Document document, String kind) {
        String prefix = ROOT + kind + "/";
        return document.select("a.section-item[href^=\"" + prefix + "\"]").stream()
                .map(anchor -> anchor.attr("href"))
                .filter(href -> href.startsWith(prefix) && href.length() > prefix.length())
                .map(href -> URLDecoder.decode(href.substring(prefix.length()), StandardCharsets.UTF_8))
                .distinct()
                .sorted()
                .toList();
    }

    public Element parseElement(Document document, String id) {
        Element result = new Element();
        populateCommon(result, document, id, "element");
        result.setAspects(parseReferenceAmounts(findField(document, "Aspects"), "element"));
        result.setHidden(parseBoolean(findField(document, "Hidden?")));
        result.setNoArtNeeded(parseBoolean(findField(document, "No Art Needed?")));
        result.setUnique(parseBoolean(findField(document, "Unique?")));
        result.setManifestationType(fieldText(findField(document, "Manifestation type")));
        ElementClassificationPolicy.classify(result);
        return result;
    }

    public Recipe parseRecipe(Document document, String id) {
        Recipe result = new Recipe();
        populateCommon(result, document, id, "recipe");
        result.setStartDescription(localizedText(findField(document, "Start Description")));
        result.setVerbIds(parseReferenceIds(findField(document, "Verb"), "verb"));
        result.setRequirements(parseReferenceAmounts(findField(document, "Requirements"), "element"));
        result.setTableRequirements(parseReferenceAmounts(findField(document, "Table Requirements"), "element"));
        result.setExtantRequirements(parseReferenceAmounts(findField(document, "Extant Requirements"), "element"));
        result.setEffects(parseReferenceAmounts(findField(document, "Effects"), "element"));
        result.setAspects(parseReferenceAmounts(findField(document, "Aspects"), "element"));
        result.setCraftable(parseBoolean(findField(document, "Craftable?")));
        return result;
    }

    public Verb parseVerb(Document document, String id) {
        Verb result = new Verb();
        populateCommon(result, document, id, "verb");
        result.setAspects(parseReferenceAmounts(findField(document, "Aspects"), "element"));
        result.setSlots(parseSlots(findField(document, "Slots")));
        return result;
    }

    private void populateCommon(com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem result,
                                Document document, String id, String kind) {
        result.setId(id);
        result.setLabel(localizedText(findField(document, "Label")));
        String description = localizedText(findField(document, "Description"));
        if ((description == null || description.isBlank()) && "recipe".equals(kind)) {
            description = localizedText(findField(document, "Start Description"));
        }
        result.setDesc(description);
        result.setSourceFile(sourceFile(document));
        result.setFields(parseFields(document));

        org.jsoup.nodes.Element image = document.selectFirst("#data-page > img.content-image");
        if (image != null) {
            result.setRowenariumImageSrc(image.attr("src"));
        }
    }

    private Map<String, String> parseFields(Document document) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (org.jsoup.nodes.Element title : document.select("#data-page strong.field-title")) {
            String name = normalizeTitle(title.text());
            org.jsoup.nodes.Element field = title.parent();
            String value = localizedText(field);
            if (value == null || value.isBlank()) {
                value = fieldText(field);
            }
            if (value != null && !value.isBlank() && !"None".equalsIgnoreCase(value)) {
                fields.putIfAbsent(name, value);
            }
        }
        return fields;
    }

    private List<VerbSlot> parseSlots(org.jsoup.nodes.Element field) {
        if (field == null) {
            return List.of();
        }
        org.jsoup.nodes.Element container = field;
        if (field.select("strong.subfield-title").isEmpty() && field.nextElementSibling() != null) {
            container = field.nextElementSibling();
        }
        List<VerbSlot> slots = new ArrayList<>();
        Elements items = new Elements();
        org.jsoup.nodes.Element list = container.tagName().equals("ul") ? container : null;
        if (list == null) {
            for (org.jsoup.nodes.Element child : container.children()) {
                if (child.tagName().equals("ul")) {
                    list = child;
                    break;
                }
            }
        }
        org.jsoup.nodes.Element itemContainer = list != null ? list : container;
        for (org.jsoup.nodes.Element child : itemContainer.children()) {
            if (child.tagName().equals("li")) {
                items.add(child);
            }
        }
        for (org.jsoup.nodes.Element item : items) {
            VerbSlot slot = new VerbSlot();
            slot.setLabel(localizedText(findSubfield(item, "Label")));
            slot.setDescription(localizedText(findSubfield(item, "Description")));
            slot.setEssential(parseReferenceIds(findSubfield(item, "Essential"), "element"));
            slot.setRequired(parseReferenceIds(findSubfield(item, "Required"), "element"));
            slot.setForbidden(parseReferenceIds(findSubfield(item, "Forbidden"), "element"));
            slot.setGreedy(parseBoolean(findSubfield(item, "Greedy?")));
            slot.setConsumes(parseBoolean(findSubfield(item, "Consumes?")));
            if (slot.getLabel() != null || !slot.getEssential().isEmpty()
                    || !slot.getRequired().isEmpty() || !slot.getForbidden().isEmpty()) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private org.jsoup.nodes.Element findField(Document document, String name) {
        for (org.jsoup.nodes.Element title : document.select("#data-page strong.field-title")) {
            if (normalizeTitle(title.text()).equalsIgnoreCase(name)) {
                return title.parent();
            }
        }
        return null;
    }

    private org.jsoup.nodes.Element findSubfield(org.jsoup.nodes.Element parent, String name) {
        for (org.jsoup.nodes.Element title : parent.select("strong.subfield-title")) {
            if (normalizeTitle(title.text()).equalsIgnoreCase(name)) {
                return title.parent();
            }
        }
        return null;
    }

    private String localizedText(org.jsoup.nodes.Element field) {
        if (field == null) {
            return null;
        }
        Elements values = field.select("li.localizations-item");
        if (values.isEmpty()) {
            org.jsoup.nodes.Element sibling = field.nextElementSibling();
            while (sibling != null) {
                if (sibling.tagName().equals("ul") && sibling.hasClass("localizations")) {
                    values = sibling.select("li.localizations-item");
                    break;
                }
                if (sibling.hasClass("content-field") && sibling.selectFirst("strong.field-title") != null) {
                    break;
                }
                sibling = sibling.nextElementSibling();
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        int chineseIndex = Math.min(2, values.size() - 1);
        return values.get(chineseIndex).text().trim();
    }

    private String fieldText(org.jsoup.nodes.Element field) {
        if (field == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (Node node : field.childNodes()) {
            if (node instanceof org.jsoup.nodes.Element element && element.hasClass("field-title")) {
                continue;
            }
            if (node instanceof TextNode textNode) {
                appendText(text, textNode.text());
            } else if (node instanceof org.jsoup.nodes.Element element) {
                appendText(text, element.text());
            }
        }
        String value = text.toString().trim();
        return value.isEmpty() || "None".equalsIgnoreCase(value) ? null : value;
    }

    private void appendText(StringBuilder target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!target.isEmpty()) {
            target.append(' ');
        }
        target.append(value.trim());
    }

    private Map<String, Integer> parseReferenceAmounts(org.jsoup.nodes.Element field, String kind) {
        Map<String, Integer> references = new LinkedHashMap<>();
        if (field == null) {
            return references;
        }
        for (org.jsoup.nodes.Element anchor : field.select("a[href^=\"" + ROOT + kind + "/\"]")) {
            org.jsoup.nodes.Element id = anchor.selectFirst("span.ref-id");
            if (id == null || id.text().isBlank()) {
                continue;
            }
            int amount = 1;
            org.jsoup.nodes.Element amountNode = anchor.selectFirst("span.ref-amount");
            if (amountNode != null) {
                try {
                    amount = Integer.parseInt(amountNode.text().trim());
                } catch (NumberFormatException ignored) {
                    amount = 1;
                }
            }
            references.merge(id.text().trim(), amount, Integer::sum);
        }
        return references;
    }

    private List<String> parseReferenceIds(org.jsoup.nodes.Element field, String kind) {
        if (field == null) {
            return List.of();
        }
        return field.select("a[href^=\"" + ROOT + kind + "/\"] span.ref-id").stream()
                .map(org.jsoup.nodes.Element::text)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private boolean parseBoolean(org.jsoup.nodes.Element field) {
        String value = fieldText(field);
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("yes") || normalized.equals("1");
    }

    private String sourceFile(Document document) {
        org.jsoup.nodes.Element active = document.selectFirst("#section-item-active");
        if (active == null) {
            return null;
        }
        org.jsoup.nodes.Element file = active.parent();
        org.jsoup.nodes.Element title = file != null ? file.selectFirst(".section-file-title") : null;
        return title != null ? title.text().trim() : null;
    }

    private String normalizeTitle(String value) {
        String normalized = value != null ? value.trim() : "";
        while (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }
}
