package com.github.alphardpaarthurnax.bohcalculator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class AspectDataService {

    private static final String SDE_PATH = "/assets/sde/aspects.json";

    private static AspectDataService instance;
    private final ObservableList<Aspect> cachedAspects = FXCollections.observableArrayList();

    private AspectDataService() {
        cachedAspects.addAll(loadFromSde());
    }

    public static AspectDataService getInstance() {
        if (instance == null) {
            instance = new AspectDataService();
        }
        return instance;
    }

    public ObservableList<Aspect> getAspects() {
        return cachedAspects;
    }

    public void reload() {
        cachedAspects.clear();
        cachedAspects.addAll(loadFromSde());
    }

    private List<Aspect> loadFromSde() {
        try (InputStream is = getClass().getResourceAsStream(SDE_PATH)) {
            if (is == null) {
                System.err.println("SDE file not found: " + SDE_PATH + " — run Data Generator first.");
                return List.of();
            }
            ObjectMapper mapper = new ObjectMapper();
            Map<String, List<Aspect>> data = mapper.readValue(is,
                    new TypeReference<Map<String, List<Aspect>>>() {});
            List<Aspect> aspects = data.get("aspects");
            if (aspects != null) {
                for (Aspect a : aspects) {
                    if (a.getImagePath() != null) {
                        InputStream imgStream = getClass().getResourceAsStream(a.getImagePath());
                        if (imgStream != null) {
                            a.setImage(new Image(imgStream));
                        }
                    }
                }
            }
            return aspects != null ? aspects : List.of();
        } catch (Exception e) {
            System.err.println("Failed to load SDE: " + e.getMessage());
            return List.of();
        }
    }
}
