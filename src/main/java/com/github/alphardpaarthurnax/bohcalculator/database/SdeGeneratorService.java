package com.github.alphardpaarthurnax.bohcalculator.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.Book;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import com.github.alphardpaarthurnax.bohcalculator.model.Comfort;
import com.github.alphardpaarthurnax.bohcalculator.model.Element;
import com.github.alphardpaarthurnax.bohcalculator.model.Item;
import com.github.alphardpaarthurnax.bohcalculator.model.Recipe;
import com.github.alphardpaarthurnax.bohcalculator.model.SdeGenerationReport;
import com.github.alphardpaarthurnax.bohcalculator.model.Thing;
import com.github.alphardpaarthurnax.bohcalculator.model.Verb;
import com.github.alphardpaarthurnax.bohcalculator.model.Wallart;
import com.github.alphardpaarthurnax.bohcalculator.model.Workstation;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class SdeGeneratorService {
    static final Path IMAGE_DIR = Path.of("src/main/resources/assets/images");
    static final Path SDE_DIR = Path.of("src/main/resources/assets/sde");

    private final ObjectMapper objectMapper;
    private final RowenariumClient client;
    private final RowenariumParser parser;

    public SdeGeneratorService() {
        this(new ObjectMapper(), new RowenariumClient(), new RowenariumParser());
    }

    SdeGeneratorService(ObjectMapper objectMapper, RowenariumClient client, RowenariumParser parser) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.parser = parser;
    }

    public SdeGenerationReport generateAll(int requestedThreads,
                                           BiConsumer<Double, String> progressCallback) throws Exception {
        return generateAll(requestedThreads, false, progressCallback);
    }

    public SdeGenerationReport generateAll(int requestedThreads, boolean refreshAll,
                                           BiConsumer<Double, String> progressCallback) throws Exception {
        int threadCount = Math.max(1, Math.min(requestedThreads, 32));
        BiConsumer<Double, String> progress = progressCallback != null
                ? progressCallback : (value, message) -> { };

        progress.accept(0.0, "从 Rowenarium 获取完整索引...");
        Document indexDocument = client.fetch("element", "xmet");
        List<String> elementIds = parser.parseIndex(indexDocument, "element");
        List<String> recipeIds = parser.parseIndex(indexDocument, "recipe");
        List<String> verbIds = parser.parseIndex(indexDocument, "verb");
        progress.accept(0.01, "索引：Element " + elementIds.size()
                + "，Recipe " + recipeIds.size() + "，Verb " + verbIds.size());

        List<String> failedPages = new ArrayList<>();
        Map<String, Element> elementCache = refreshAll ? Map.of()
                : loadExisting("elements.json", "elements", Element.class);
        Map<String, Recipe> recipeCache = refreshAll ? Map.of()
                : loadExisting("recipes.json", "recipes", Recipe.class);
        Map<String, Verb> verbCache = refreshAll ? Map.of()
                : loadExisting("verbs.json", "verbs", Verb.class);

        List<Element> elements = fetchAll("Element", "element", elementIds, elementCache, threadCount, 0.01, 0.43,
                parser::parseElement, failedPages, progress);
        List<Recipe> recipes = fetchAll("Recipe", "recipe", recipeIds, recipeCache, threadCount, 0.43, 0.90,
                parser::parseRecipe, failedPages, progress);
        List<Verb> verbs = fetchAll("Verb", "verb", verbIds, verbCache, threadCount, 0.90, 0.96,
                parser::parseVerb, failedPages, progress);

        progress.accept(0.961, "按 Rowenarium 字段派生分类数据...");
        for (Element element : elements) {
            element.setImagePath(null);
            ElementClassificationPolicy.classify(element);
        }
        recipes.forEach(recipe -> {
            recipe.setImagePath(null);
            recipe.setRowenariumImageSrc(null);
        });
        verbs.forEach(verb -> verb.setImagePath(null));

        List<Aspect> aspects = new ArrayList<>(elements.stream()
                .filter(Element::isAspect)
                .map(this::toAspect)
                .sorted(displayComparator())
                .toList());
        List<Card> cards = new ArrayList<>();
        List<Book> books = new ArrayList<>();
        List<Wallart> wallarts = new ArrayList<>();
        List<Comfort> comforts = new ArrayList<>();
        List<Thing> things = new ArrayList<>();
        for (Element element : elements) {
            if (!ElementClassificationPolicy.isCard(element)) {
                continue;
            }
            switch (ItemClassificationPolicy.classify(element)) {
                case BOOK -> books.add(toCard(element, new Book()));
                case WALLART -> wallarts.add(toCard(element, new Wallart()));
                case COMFORT -> comforts.add(toCard(element, new Comfort()));
                case THING -> things.add(toCard(element, new Thing()));
                case NONE -> cards.add(toCard(element, new Card()));
            }
        }
        cards.sort(displayComparator());
        books.sort(displayComparator());
        wallarts.sort(displayComparator());
        comforts.sort(displayComparator());
        things.sort(displayComparator());
        List<Recipe> crafts = recipes.stream().filter(this::isCraft).sorted(idComparator()).toList();
        List<Recipe> otherRecipes = recipes.stream().filter(recipe -> !isCraft(recipe)).sorted(idComparator()).toList();
        List<Workstation> workstations = deriveWorkstations(verbs, crafts);
        Set<String> workstationIds = workstations.stream().map(Workstation::getId).collect(java.util.stream.Collectors.toSet());
        List<Verb> otherVerbs = verbs.stream()
                .filter(verb -> !workstationIds.contains(verb.getId()))
                .sorted(idComparator())
                .toList();

        List<String> downloadedImages = new ArrayList<>();
        List<String> missingImages = new ArrayList<>();
        Map<String, List<Path>> localImages = indexLocalImages();
        resolveImages(aspects, "aspects", threadCount, localImages, downloadedImages, missingImages,
                0.962, 0.971, progress);
        resolveImages(cards, "cards", threadCount, localImages, downloadedImages, missingImages,
                0.971, 0.980, progress);
        resolveImages(books, "books", threadCount, localImages, downloadedImages, missingImages,
                0.980, 0.986, progress);
        resolveImages(wallarts, "wallarts", threadCount, localImages, downloadedImages, missingImages,
                0.986, 0.990, progress);
        resolveImages(comforts, "comforts", threadCount, localImages, downloadedImages, missingImages,
                0.990, 0.993, progress);
        resolveImages(things, "things", threadCount, localImages, downloadedImages, missingImages,
                0.993, 0.995, progress);
        resolveImages(workstations, "workstations", threadCount, localImages, downloadedImages, missingImages,
                0.995, 0.997, progress);

        List<CatalogItem> resolvedElements = new ArrayList<>();
        resolvedElements.addAll(aspects);
        resolvedElements.addAll(cards);
        resolvedElements.addAll(books);
        resolvedElements.addAll(wallarts);
        resolvedElements.addAll(comforts);
        resolvedElements.addAll(things);
        syncImageMetadata(elements, resolvedElements);
        syncImageMetadata(verbs, workstations);
        aspects.removeIf(item -> !ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
        cards.removeIf(item -> !ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
        books.removeIf(item -> !ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
        wallarts.removeIf(item -> !ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
        comforts.removeIf(item -> !ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
        things.removeIf(item -> !ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc()));
        List<Item> items = new ArrayList<>();
        items.addAll(books);
        items.addAll(wallarts);
        items.addAll(comforts);
        items.addAll(things);
        items.sort(displayComparator());

        elements.sort(idComparator());
        recipes.sort(idComparator());
        verbs.sort(idComparator());

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("elements", elements.size());
        counts.put("recipes", recipes.size());
        counts.put("verbs", verbs.size());
        counts.put("aspects", aspects.size());
        counts.put("cards", cards.size());
        counts.put("items", items.size());
        counts.put("books", books.size());
        counts.put("wallarts", wallarts.size());
        counts.put("comforts", comforts.size());
        counts.put("things", things.size());
        counts.put("crafts", crafts.size());
        counts.put("workstations", workstations.size());
        counts.put("otherRecipes", otherRecipes.size());
        counts.put("otherVerbs", otherVerbs.size());
        SdeGenerationReport report = new SdeGenerationReport(
                Instant.now().toString(), counts, List.copyOf(failedPages),
                List.copyOf(downloadedImages), List.copyOf(missingImages));

        Files.createDirectories(SDE_DIR);
        writeWrapped("elements.json", "elements", elements);
        writeWrapped("recipes.json", "recipes", recipes);
        writeWrapped("verbs.json", "verbs", verbs);
        writeWrapped("aspects.json", "aspects", aspects);
        writeWrapped("cards.json", "cards", cards);
        writeWrapped("items.json", "items", items);
        writeWrapped("books.json", "books", books);
        writeWrapped("wallarts.json", "wallarts", wallarts);
        writeWrapped("comforts.json", "comforts", comforts);
        writeWrapped("things.json", "things", things);
        writeWrapped("crafts.json", "crafts", crafts);
        writeWrapped("workstations.json", "workstations", workstations);
        writeWrapped("other-recipes.json", "otherRecipes", otherRecipes);
        writeWrapped("other-verbs.json", "otherVerbs", otherVerbs);
        writeValue("generation-report.json", report);

        String status = report.complete() ? "SDE 分类数据生成完成" : "SDE 已生成，但有页面抓取失败";
        progress.accept(1.0, status + "；" + counts + "；下载图片 " + downloadedImages.size()
                + "；缺图 " + missingImages.size() + "；失败页面 " + failedPages.size());
        return report;
    }

    private boolean isCraft(Recipe recipe) {
        String id = recipe.getId() != null ? recipe.getId().toLowerCase(Locale.ROOT) : "";
        return id.startsWith("craft.") || id.startsWith("remove.");
    }

    private List<Workstation> deriveWorkstations(List<Verb> verbs, List<Recipe> crafts) {
        Map<String, List<String>> craftIdsByVerb = new HashMap<>();
        for (Recipe craft : crafts) {
            if (!craft.isCraftable()) {
                continue;
            }
            for (String verbId : craft.getVerbIds()) {
                craftIdsByVerb.computeIfAbsent(verbId, ignored -> new ArrayList<>()).add(craft.getId());
            }
        }
        List<Workstation> result = new ArrayList<>();
        for (Verb verb : verbs) {
            List<String> craftIds = craftIdsByVerb.get(verb.getId());
            if (craftIds == null || craftIds.isEmpty()) {
                continue;
            }
            Workstation workstation = new Workstation();
            copyCommon(verb, workstation);
            workstation.setAspects(new LinkedHashMap<>(verb.getAspects()));
            workstation.setSlots(new ArrayList<>(verb.getSlots()));
            workstation.setRecipeIds(craftIds.stream().sorted().toList());
            result.add(workstation);
        }
        result.sort(displayComparator());
        return result;
    }

    private void resolveImages(List<? extends CatalogItem> items, String targetFolder, int threadCount,
                               Map<String, List<Path>> localImages, List<String> downloaded,
                               List<String> missing, double phaseStart, double phaseEnd,
                               BiConsumer<Double, String> progress) throws InterruptedException, IOException {
        Map<String, List<CatalogItem>> downloads = new LinkedHashMap<>();
        for (CatalogItem item : items) {
            item.setImagePath(null);
            if (!ElementClassificationPolicy.hasNormalImage(item.getRowenariumImageSrc())) {
                if ("workstations".equals(targetFolder)) {
                    usePlaceholder(item, true);
                }
                continue;
            }
            String fileName = imageFileName(item.getRowenariumImageSrc());
            if (fileName == null || fileName.equalsIgnoreCase("_x.png")) {
                continue;
            }
            List<Path> matches = localImages.getOrDefault(fileName.toLowerCase(Locale.ROOT), List.of());
            if (!matches.isEmpty()) {
                item.setImagePath(toResourcePath(matches.getFirst()));
            } else {
                downloads.computeIfAbsent(fileName, ignored -> new ArrayList<>()).add(item);
            }
        }

        progress.accept(phaseStart, targetFolder + "：需从网站下载 " + downloads.size() + " 张图片");
        if (downloads.isEmpty()) {
            progress.accept(phaseEnd, targetFolder + "：图片检查完成");
            return;
        }
        Path targetDir = IMAGE_DIR.resolve(targetFolder).normalize();
        Files.createDirectories(targetDir);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CompletionService<ImageDownloadResult> completion = new ExecutorCompletionService<>(executor);
        downloads.forEach((fileName, owners) -> completion.submit(() -> {
            CatalogItem owner = owners.getFirst();
            Path target = targetDir.resolve(fileName).normalize();
            if (!target.startsWith(targetDir)) {
                return new ImageDownloadResult(fileName, null, "非法图片文件名", false);
            }
            try {
                byte[] bytes = client.downloadImage(owner.getRowenariumImageSrc());
                Path temporary = targetDir.resolve(fileName + ".tmp");
                Files.write(temporary, bytes);
                moveAtomically(temporary, target);
                return new ImageDownloadResult(fileName, target, null, false);
            } catch (RowenariumClient.ImageUnavailableException exception) {
                return new ImageDownloadResult(fileName, null, null, true);
            } catch (Exception exception) {
                return new ImageDownloadResult(fileName, null, exception.getMessage(), false);
            }
        }));

        try {
            for (int completed = 1; completed <= downloads.size(); completed++) {
                ImageDownloadResult result = completion.take().get();
                double overall = phaseStart + (phaseEnd - phaseStart) * completed / downloads.size();
                if (result.placeholder()) {
                    downloads.get(result.fileName()).forEach(item -> {
                        usePlaceholder(item, "workstations".equals(targetFolder));
                    });
                } else if (result.error() == null) {
                    String resourcePath = toResourcePath(result.path());
                    downloads.get(result.fileName()).forEach(item -> item.setImagePath(resourcePath));
                    downloaded.add(targetFolder + "/" + result.fileName());
                } else {
                    missing.add(targetFolder + "/" + result.fileName() + "：" + result.error());
                }
                if (completed == downloads.size() || completed % 10 == 0) {
                    progress.accept(overall, targetFolder + " 图片 [" + completed + "/" + downloads.size() + "]");
                }
            }
        } catch (java.util.concurrent.ExecutionException impossible) {
            throw new IllegalStateException("Unexpected image task failure", impossible);
        } finally {
            executor.shutdownNow();
        }
    }

    private void usePlaceholder(CatalogItem item, boolean workstation) {
        String source = item.getRowenariumImageSrc();
        int slash = source != null ? source.lastIndexOf('/') : -1;
        if (slash >= 0) {
            item.setRowenariumImageSrc(source.substring(0, slash + 1) + "_x.png");
        } else if (workstation) {
            item.setRowenariumImageSrc("/rowenarium/static/bhimages/verbs/_x.png");
        }
        item.setImagePath(workstation ? "/assets/images/verbs/_x.png" : null);
    }

    private void syncImageMetadata(List<? extends CatalogItem> targets,
                                   List<? extends CatalogItem> resolvedItems) {
        Map<String, CatalogItem> byId = new HashMap<>();
        targets.forEach(item -> byId.put(item.getId(), item));
        for (CatalogItem resolved : resolvedItems) {
            CatalogItem target = byId.get(resolved.getId());
            if (target != null) {
                target.setRowenariumImageSrc(resolved.getRowenariumImageSrc());
            }
        }
    }

    private <T extends CatalogItem> List<T> fetchAll(String displayKind, String urlKind, List<String> ids,
                                                       Map<String, T> cache, int threadCount,
                                                       double phaseStart, double phaseEnd,
                                                       BiFunction<Document, String, T> pageParser,
                                                       List<String> failures,
                                                       BiConsumer<Double, String> progress) throws InterruptedException {
        List<T> results = new ArrayList<>(ids.size());
        List<String> fetchIds = new ArrayList<>();
        for (String id : ids) {
            T cached = cache.get(id);
            if (cached != null) {
                results.add(cached);
            } else {
                fetchIds.add(id);
            }
        }
        int cachedCount = results.size();
        progress.accept(phaseStart, displayKind + " 共 " + ids.size() + "；复用 " + cachedCount
                + "，需抓取 " + fetchIds.size());
        if (fetchIds.isEmpty()) {
            progress.accept(phaseEnd, displayKind + " 已全部复用");
            return results;
        }
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CompletionService<FetchResult<T>> completion = new ExecutorCompletionService<>(executor);
        for (String id : fetchIds) {
            completion.submit(() -> {
                try {
                    return new FetchResult<>(id, pageParser.apply(client.fetch(urlKind, id), id), null);
                } catch (Exception exception) {
                    return new FetchResult<>(id, null, exception);
                }
            });
        }
        try {
            for (int completed = 1; completed <= fetchIds.size(); completed++) {
                FetchResult<T> result = completion.take().get();
                double overall = phaseStart + (phaseEnd - phaseStart) * (cachedCount + completed) / ids.size();
                if (result.error() == null) {
                    results.add(result.value());
                } else {
                    failures.add(urlKind + "/" + result.id() + "：" + result.error().getMessage());
                }
                if (completed == fetchIds.size() || completed % 20 == 0) {
                    progress.accept(overall, displayKind + " [新增 " + completed + "/" + fetchIds.size() + "]");
                }
            }
        } catch (java.util.concurrent.ExecutionException impossible) {
            throw new IllegalStateException("Unexpected fetch task failure", impossible);
        } finally {
            executor.shutdownNow();
        }
        return results;
    }

    private <T extends CatalogItem> Map<String, T> loadExisting(String fileName, String rootKey, Class<T> type) {
        Path file = SDE_DIR.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        try {
            JsonNode values = objectMapper.readTree(file.toFile()).path(rootKey);
            var listType = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
            List<T> items = objectMapper.convertValue(values, listType);
            Map<String, T> byId = new LinkedHashMap<>();
            items.forEach(item -> byId.put(item.getId(), item));
            return byId;
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Map<String, List<Path>> indexLocalImages() throws IOException {
        Map<String, List<Path>> result = new LinkedHashMap<>();
        if (!Files.isDirectory(IMAGE_DIR)) {
            return result;
        }
        try (Stream<Path> files = Files.walk(IMAGE_DIR)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                result.computeIfAbsent(file.getFileName().toString().toLowerCase(Locale.ROOT),
                        ignored -> new ArrayList<>()).add(file);
            }
        }
        return result;
    }

    private String imageFileName(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String raw = source.substring(source.lastIndexOf('/') + 1);
        try {
            return URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return raw;
        }
    }

    private String toResourcePath(Path image) {
        return "/assets/images/" + IMAGE_DIR.relativize(image).toString().replace('\\', '/');
    }

    private Aspect toAspect(Element element) {
        Aspect result = new Aspect();
        copyCommon(element, result);
        return result;
    }

    private <T extends Card> T toCard(Element element, T result) {
        copyCommon(element, result);
        result.setAspects(new LinkedHashMap<>(element.getAspects()));
        return result;
    }

    private void copyCommon(CatalogItem source, CatalogItem target) {
        target.setId(source.getId());
        target.setLabel(source.getLabel());
        target.setDesc(source.getDesc());
        target.setImagePath(source.getImagePath());
        target.setRowenariumImageSrc(source.getRowenariumImageSrc());
        target.setSourceFile(source.getSourceFile());
        target.setFields(new LinkedHashMap<>(source.getFields()));
    }

    private <T extends CatalogItem> Comparator<T> displayComparator() {
        return Comparator.<T, String>comparing(CatalogItem::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CatalogItem::getId);
    }

    private <T extends CatalogItem> Comparator<T> idComparator() {
        return Comparator.comparing(CatalogItem::getId, String.CASE_INSENSITIVE_ORDER);
    }

    private void writeWrapped(String fileName, String key, List<?> values) throws IOException {
        writeValue(fileName, Map.of(key, values));
    }

    private void writeValue(String fileName, Object value) throws IOException {
        Path output = SDE_DIR.resolve(fileName);
        Path temporary = SDE_DIR.resolve(fileName + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value);
        moveAtomically(temporary, output);
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record FetchResult<T>(String id, T value, Exception error) { }

    private record ImageDownloadResult(String fileName, Path path, String error, boolean placeholder) { }
}
