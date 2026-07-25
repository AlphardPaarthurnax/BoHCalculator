package com.github.alphardpaarthurnax.bohcalculator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AspectDataService {

    private static final String ELEMENTS_DIR = "/assets/content/aspects/";
    private static final String IMAGES_DIR = "/assets/images/aspects/";

    private final ObjectMapper objectMapper;

    public AspectDataService() {
        this.objectMapper = new ObjectMapper();
    }

    public List<Aspect> loadAllElements() {
        List<Aspect> allElements = new ArrayList<>();

        try {
            URL dirUrl = getClass().getResource(ELEMENTS_DIR);
            if (dirUrl != null) {
                Path dirPath = Paths.get(dirUrl.toURI());
                try (var stream = Files.list(dirPath)) {
                    List<Path> jsonFiles = stream
                            .filter(p -> p.toString().endsWith(".json"))
                            .sorted()
                            .collect(Collectors.toList());

                    for (Path jsonFile : jsonFiles) {
                        String fileName = jsonFile.getFileName().toString();
                        try (InputStream is = Files.newInputStream(jsonFile)) {
                            Map<String, List<Aspect>> data = objectMapper.readValue(is,
                                    new TypeReference<Map<String, List<Aspect>>>() {});
                            List<Aspect> elements = data.get("elements");
                            if (elements != null) {
                                for (Aspect element : elements) {
                                    element.setSourceFile(fileName);
                                }
                                allElements.addAll(elements);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse: " + fileName + " - " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Aspect element : allElements) {
            if (element.getId() != null && !element.getId().isEmpty()) {
                String imageName = resolveImageName(element.getId());
                String imagePath = IMAGES_DIR + imageName + ".png";
                InputStream is = getClass().getResourceAsStream(imagePath);
                if (is != null) {
                    element.setImage(new Image(is));
                }
            }
        }

        allElements.removeIf(e -> e.getImage() == null);

        return allElements;
    }

    private static String resolveImageName(String id) {
        if (id.equals("fee")){
            return "pence";
        }
        if (id.equals("workstation")){
            return "sortgroup";
        }
        if (id.equals("ability.setup")){
            return "ability";
        }
        if (id.equals("lyterion")){
            return "r.insects.nectars";
        }
        if (id.equals("dissatisfying")){
            return "resentment";
        }
        if (id.equals("kod")){
            return "pentiment";
        }
        if (id.equals("reading.correspondence")){
            return "reading.knock";
        }
        if (id.equals("campable")){
            return "mybed";
        }
        if (id.startsWith("orderplaced.")) {
            return "orderplaced";
        }
        if (id.startsWith("contains.")){
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
