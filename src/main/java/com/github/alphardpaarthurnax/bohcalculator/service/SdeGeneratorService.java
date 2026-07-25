package com.github.alphardpaarthurnax.bohcalculator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SdeGeneratorService {

    private static final String ASPECTS_CONTENT_DIR = "src/main/resources/assets/content/aspects";
    private static final String OTHER_CONTENT_DIR = "src/main/resources/assets/content/other";
    private static final String ELEMENTS_IMAGES_DIR = "src/main/resources/assets/images/elements";
    private static final String SDE_DIR = "src/main/resources/assets/sde";
    private static final String ASPECTS_SDE_FILE = "aspects.json";
    private static final String CARDS_SDE_FILE = "cards.json";
    private static final String IMAGES_CLASSPATH_PREFIX = "/assets/images/";
    private static final String ROWENARIUM_URL = "https://uadaf.theevilroot.xyz/rowenarium/element/";

    private final ObjectMapper objectMapper;

    public SdeGeneratorService() {
        this.objectMapper = new ObjectMapper();
    }

    public void generateAspects(BiConsumer<Integer, String> progressCallback) throws Exception {
        progressCallback.accept(0, "Loading aspect JSON files...");

        List<Aspect> allAspects = new ArrayList<>();
        Path aspectsDir = Paths.get(ASPECTS_CONTENT_DIR);
        if (Files.isDirectory(aspectsDir)) {
            List<Path> jsonFiles;
            try (var stream = Files.list(aspectsDir)) {
                jsonFiles = stream.filter(p -> p.toString().endsWith(".json")).sorted().collect(Collectors.toList());
            }
            for (Path jsonFile : jsonFiles) {
                String fileName = jsonFile.getFileName().toString();
                try (InputStream is = Files.newInputStream(jsonFile)) {
                    Map<String, List<Aspect>> data = objectMapper.readValue(is,
                            new TypeReference<Map<String, List<Aspect>>>() {});
                    List<Aspect> elements = data.get("elements");
                    if (elements != null) {
                        for (Aspect a : elements) {
                            a.setSourceFile(fileName);
                        }
                        allAspects.addAll(elements);
                    }
                } catch (Exception e) {
                    progressCallback.accept(0, "WARN: Failed to parse " + fileName);
                }
            }
        }

        progressCallback.accept(0, "Resolving images for " + allAspects.size() + " aspects...");

        List<Aspect> validAspects = new ArrayList<>();
        for (Aspect a : allAspects) {
            if (a.getId() != null && !a.getId().isEmpty()) {
                String imageName = resolveAspectImageName(a.getId());
                Path imageFile = Paths.get("src/main/resources/assets/images/aspects", imageName + ".png");
                if (Files.exists(imageFile)) {
                    a.setImagePath(IMAGES_CLASSPATH_PREFIX + "aspects/" + imageName + ".png");
                    validAspects.add(a);
                }
            }
        }

        Map<String, List<Aspect>> wrapper = new LinkedHashMap<>();
        wrapper.put("aspects", validAspects);

        Files.createDirectories(Paths.get(SDE_DIR));
        Path outFile = Paths.get(SDE_DIR, ASPECTS_SDE_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outFile.toFile(), wrapper);

        progressCallback.accept(100, "Generated " + ASPECTS_SDE_FILE + " with " + validAspects.size() + " aspects.");
    }

    public void generateCards(int threadCount, BiConsumer<Double, String> progressCallback) throws Exception {
        progressCallback.accept(0.0, "Loading other JSON files...");

        List<Aspect> rawCards = new ArrayList<>();
        Path otherDir = Paths.get(OTHER_CONTENT_DIR);
        if (Files.isDirectory(otherDir)) {
            List<Path> jsonFiles;
            try (var stream = Files.list(otherDir)) {
                jsonFiles = stream.filter(p -> p.toString().endsWith(".json")).sorted().collect(Collectors.toList());
            }
            for (Path jsonFile : jsonFiles) {
                String fileName = jsonFile.getFileName().toString();
                try (InputStream is = Files.newInputStream(jsonFile)) {
                    Map<String, List<Aspect>> data = objectMapper.readValue(is,
                            new TypeReference<Map<String, List<Aspect>>>() {});
                    List<Aspect> elements = data.get("elements");
                    if (elements != null) {
                        for (Aspect a : elements) {
                            a.setSourceFile(fileName);
                        }
                        rawCards.addAll(elements);
                    }
                } catch (Exception e) {
                    progressCallback.accept(0.0, "WARN: Failed to parse " + fileName);
                }
            }
        }

        int total = rawCards.size();
        progressCallback.accept(0.0, "Fetching " + total + " cards from Rowenarium (" + threadCount + " threads)...");

        List<Card> cards = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (Aspect raw : rawCards) {
            if (raw.getId() == null || raw.getId().isEmpty()) {
                completed.incrementAndGet();
                continue;
            }
            executor.submit(() -> {
                try {
                    Card card = fetchCardFromRowenarium(raw);
                    if (card != null) {
                        synchronized (cards) {
                            cards.add(card);
                        }
                        int sc = successCount.incrementAndGet();
                        progressCallback.accept((double) completed.incrementAndGet() / total,
                                "[" + completed.get() + "/" + total + "] " + card.getId() + " OK (" + card.getAspects().size() + " aspects)");
                    } else {
                        int c = completed.incrementAndGet();
                        progressCallback.accept((double) c / total,
                                "[" + c + "/" + total + "] " + raw.getId() + " SKIPPED (no aspects)");
                    }
                } catch (Exception e) {
                    int c = completed.incrementAndGet();
                    progressCallback.accept((double) c / total,
                            "[" + c + "/" + total + "] " + raw.getId() + " FAILED: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(200);
        }

        cards.sort((a, b) -> {
            String la = a.getDisplayName();
            String lb = b.getDisplayName();
            return java.text.Collator.getInstance(java.util.Locale.CHINA).compare(la, lb);
        });

        Map<String, List<Card>> wrapper = new LinkedHashMap<>();
        wrapper.put("cards", cards);

        Files.createDirectories(Paths.get(SDE_DIR));
        Path outFile = Paths.get(SDE_DIR, CARDS_SDE_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outFile.toFile(), wrapper);

        progressCallback.accept(1.0, "Generated " + CARDS_SDE_FILE + " with " + cards.size() + " cards.");
    }

    private Card fetchCardFromRowenarium(Aspect raw) {
        String id = raw.getId();
        try {
            Document doc = Jsoup.connect(ROWENARIUM_URL + id).timeout(10000).get();

            // Parse aspects
            Map<String, Integer> aspects = new LinkedHashMap<>();
            Elements refs = doc.select("p.content-field:has(strong.field-title:containsOwn(Aspects:)) span.element-ref.ref-list a.element-ref.ref");
            for (org.jsoup.nodes.Element ref : refs) {
                org.jsoup.nodes.Element refIdSpan = ref.selectFirst("span.ref-id");
                if (refIdSpan == null) continue;
                String aspectId = refIdSpan.text().trim();
                if (aspectId.isEmpty()) continue;

                int amount = 1;
                org.jsoup.nodes.Element amountSpan = ref.selectFirst("span.ref-amount");
                if (amountSpan != null) {
                    try {
                        amount = Integer.parseInt(amountSpan.text().trim());
                    } catch (NumberFormatException ignored) {}
                }
                aspects.put(aspectId, aspects.getOrDefault(aspectId, 0) + amount);
            }

            if (aspects.isEmpty()) {
                return null;
            }

            // Parse image
            org.jsoup.nodes.Element imgEl = doc.selectFirst("img.content-image.image-element");
            String imageFileName = null;
            if (imgEl != null) {
                String src = imgEl.attr("src");
                if (src != null && src.contains("/")) {
                    imageFileName = src.substring(src.lastIndexOf('/') + 1);
                }
            }
            if (imageFileName == null || imageFileName.isEmpty() || imageFileName.equals("_x.png")) {
                imageFileName = id + ".png";
            }

            // Check if image exists locally
            Path localImage = Paths.get(ELEMENTS_IMAGES_DIR, imageFileName);
            String imagePath = null;
            if (Files.exists(localImage)) {
                imagePath = IMAGES_CLASSPATH_PREFIX + "elements/" + imageFileName;
            } else {
                // Try alternative: use id.png
                localImage = Paths.get(ELEMENTS_IMAGES_DIR, id + ".png");
                if (Files.exists(localImage)) {
                    imagePath = IMAGES_CLASSPATH_PREFIX + "elements/" + id + ".png";
                }
            }

            if (imagePath == null) {
                return null;
            }

            Card card = new Card();
            card.setId(id);
            card.setLabel(raw.getLabel());
            card.setDesc(raw.getDesc());
            card.setImagePath(imagePath);
            card.setSourceFile(raw.getSourceFile());
            card.setAspects(aspects);
            return card;

        } catch (IOException e) {
            return null;
        }
    }

    private static String resolveAspectImageName(String id) {
        if (id.equals("fee")) {
            return "pence";
        }
        if (id.equals("workstation")) {
            return "sortgroup";
        }
        if (id.equals("ability.setup")) {
            return "ability";
        }
        if (id.equals("lyterion")) {
            return "r.insects.nectars";
        }
        if (id.equals("dissatisfying")) {
            return "resentment";
        }
        if (id.equals("kod")) {
            return "pentiment";
        }
        if (id.equals("reading.correspondence")) {
            return "reading.knock";
        }
        if (id.equals("campable")) {
            return "mybed";
        }
        if (id.startsWith("orderplaced.")) {
            return "orderplaced";
        }
        if (id.startsWith("contains.")) {
            return "contains";
        }
        if (id.startsWith("interest.")) {
            return id.substring("interest.".length());
        }
        if (id.startsWith("relevance.")) {
            return id.substring("relevance.".length());
        }
        if (id.startsWith("inspiring.")) {
            return id.substring("inspiring.".length());
        }
        if (id.startsWith("group")) {
            return "sortgroup";
        }
        if (id.startsWith("h.")) {
            return "h";
        }
        if (id.startsWith("e.")) {
            return "w." + id.substring("e.".length());
        }
        if (id.startsWith("ability.exposed.")) {
            return "contamination." + id.substring("ability.exposed.".length());
        }
        if (id.startsWith("acted.")) {
            return resolveActed(id);
        }
        if (id.startsWith("memories.")) {
            return resolveMemories(id);
        }
        if (id.contains(".likes.")) {
            return "befriend";
        }
        return id;
    }

    private static String resolveActed(String id) {
        String afterActed = id.substring("acted.".length());
        int dotIndex = afterActed.indexOf('.');
        return dotIndex > 0 ? afterActed.substring(0, dotIndex) : afterActed;
    }

    private static String resolveMemories(String id) {
        switch (id) {
            case "memories.cartographer":
                return "jm.nyctodromy";
            case "memories.executioner":
                return "jm.preservation";
            case "memories.revolutionary":
                return "jm.horomachistry";
            case "memories.archaeologist":
                return "jm.skolekosophy";
            case "memories.symurgist":
                return "jm.birdsong";
            case "memories.magnate":
                return "jm.hushery";
            case "memories.twiceborn":
                return "jm.illumination";
            case "memories.prodigal":
                return "jm.ithastry";
            case "memories.artist":
                return "jm.bosk";
        }
        return id;
    }
}
