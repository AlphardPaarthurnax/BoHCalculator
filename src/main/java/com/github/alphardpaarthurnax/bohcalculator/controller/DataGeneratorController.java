package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.service.AspectDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.CardDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.SdeGeneratorService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class DataGeneratorController {

    @FXML
    private Button generateAspectsBtn;

    @FXML
    private Button generateCardsBtn;

    @FXML
    private Label aspectsStatus;

    @FXML
    private Label cardsStatus;

    @FXML
    private ProgressBar cardsProgress;

    @FXML
    private TextField threadCountField;

    @FXML
    private TextFlow logFlow;

    private final SdeGeneratorService generatorService = new SdeGeneratorService();

    @FXML
    public void initialize() {
        cardsProgress.setProgress(0);
        threadCountField.setText("16");
    }

    @FXML
    private void onGenerateAspects() {
        generateAspectsBtn.setDisable(true);
        aspectsStatus.setText("Generating...");
        logFlow.getChildren().clear();

        new Thread(() -> {
            try {
                generatorService.generateAspects((pct, msg) -> {
                    Platform.runLater(() -> {
                        appendLog(msg);
                        if (pct >= 100) {
                            aspectsStatus.setText("Done");
                            generateAspectsBtn.setDisable(false);
                            AspectDataService.getInstance().reload();
                        }
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    appendLog("ERROR: " + e.getMessage());
                    aspectsStatus.setText("Failed");
                    generateAspectsBtn.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void onGenerateCards() {
        int threadCount;
        try {
            threadCount = Integer.parseInt(threadCountField.getText().trim());
            if (threadCount < 1) threadCount = 1;
        } catch (NumberFormatException e) {
            threadCount = 16;
        }

        generateCardsBtn.setDisable(true);
        cardsStatus.setText("Generating...");
        cardsProgress.setProgress(0);
        logFlow.getChildren().clear();

        final int tc = threadCount;
        new Thread(() -> {
            try {
                generatorService.generateCards(tc, (pct, msg) -> {
                    Platform.runLater(() -> {
                        cardsProgress.setProgress(pct);
                        appendLog(msg);
                        if (pct >= 1.0) {
                            cardsStatus.setText("Done");
                            generateCardsBtn.setDisable(false);
                            AspectDataService.getInstance().reload();
                            CardDataService.getInstance().reload();
                        }
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    appendLog("ERROR: " + e.getMessage());
                    cardsStatus.setText("Failed");
                    generateCardsBtn.setDisable(false);
                });
            }
        }).start();
    }

    private void appendLog(String msg) {
        Text text = new Text(msg + "\n");
        text.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        if (msg.contains(" OK ") || msg.contains(" OK\n")) {
            text.setFill(Color.GREEN);
        } else if (msg.contains(" SKIPPED ")) {
            text.setFill(Color.DARKGOLDENROD);
        } else if (msg.contains(" FAILED:") || msg.contains("ERROR:")) {
            text.setFill(Color.RED);
        } else {
            text.setFill(Color.GRAY);
        }
        logFlow.getChildren().add(text);
    }
}
