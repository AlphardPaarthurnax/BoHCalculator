package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.SdeGenerationReport;
import com.github.alphardpaarthurnax.bohcalculator.service.AspectDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.CardDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.CraftDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.ElementDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.RecipeDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.OtherRecipeDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.OtherVerbDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.SdeGeneratorService;
import com.github.alphardpaarthurnax.bohcalculator.service.VerbDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.WorkstationDataService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class DataGeneratorController {
    @FXML private Button generateAllButton;
    @FXML private Label generationStatus;
    @FXML private Label summaryLabel;
    @FXML private ProgressBar generationProgress;
    @FXML private TextField threadCountField;
    @FXML private CheckBox refreshAllCheckbox;
    @FXML private TextFlow logFlow;

    private final SdeGeneratorService generatorService = new SdeGeneratorService();

    @FXML
    public void initialize() {
        generationProgress.setProgress(0);
        threadCountField.setText("8");
        generationStatus.setText("尚未运行");
    }

    @FXML
    private void onGenerateAll() {
        int threadCount = parseThreadCount();
        boolean refreshAll = refreshAllCheckbox.isSelected();
        generateAllButton.setDisable(true);
        threadCountField.setDisable(true);
        generationProgress.setProgress(0);
        generationStatus.setText("正在生成…");
        summaryLabel.setText("");
        logFlow.getChildren().clear();
        appendLog("开始完整 SDE 生成；可在此页面观察全过程。", Color.DODGERBLUE);

        Thread worker = new Thread(() -> {
            try {
                SdeGenerationReport report = generatorService.generateAll(threadCount, refreshAll, (progress, message) ->
                        Platform.runLater(() -> {
                            generationProgress.setProgress(progress);
                            appendLog(message, colorFor(message));
                        }));
                Platform.runLater(() -> finish(report));
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    generationStatus.setText("生成失败");
                    appendLog("错误：" + exception.getMessage(), Color.RED);
                    setControlsDisabled(false);
                });
            }
        }, "sde-generator");
        worker.setDaemon(true);
        worker.start();
    }

    private void finish(SdeGenerationReport report) {
        reloadCatalogs();
        generationProgress.setProgress(1.0);
        if (!report.complete()) {
            generationStatus.setText("已生成，但存在抓取失败");
        } else if (!report.missingImages().isEmpty()) {
            generationStatus.setText("数据生成完成，仍有缺图");
        } else {
            generationStatus.setText("完整生成成功");
        }
        summaryLabel.setText(report.counts() + "　下载图片 " + report.downloadedImages().size()
                + "　缺图 " + report.missingImages().size()
                + "　失败页面 " + report.failedPages().size());
        appendLog("报告：src/main/resources/assets/sde/generation-report.json",
                report.complete() && report.missingImages().isEmpty() ? Color.FORESTGREEN : Color.DARKORANGE);
        if (!report.missingImages().isEmpty()) {
            appendLog("仍缺图片（" + report.missingImages().size() + "）：", Color.DARKORANGE);
            report.missingImages().forEach(image -> appendLog(image, Color.DARKORANGE));
        }
        setControlsDisabled(false);
    }

    private void reloadCatalogs() {
        AspectDataService.getInstance().reload();
        CardDataService.getInstance().reload();
        CraftDataService.getInstance().reload();
        ElementDataService.getInstance().reload();
        RecipeDataService.getInstance().reload();
        OtherRecipeDataService.getInstance().reload();
        OtherVerbDataService.getInstance().reload();
        VerbDataService.getInstance().reload();
        WorkstationDataService.getInstance().reload();
    }

    private int parseThreadCount() {
        try {
            return Math.max(1, Math.min(32, Integer.parseInt(threadCountField.getText().trim())));
        } catch (NumberFormatException ignored) {
            return 8;
        }
    }

    private void setControlsDisabled(boolean disabled) {
        generateAllButton.setDisable(disabled);
        threadCountField.setDisable(disabled);
        refreshAllCheckbox.setDisable(disabled);
    }

    private Color colorFor(String message) {
        if (message.startsWith("失败") || message.startsWith("错误")) {
            return Color.RED;
        }
        if (message.startsWith("缺少图片")) {
            return Color.DARKORANGE;
        }
        if (message.contains("完成")) {
            return Color.FORESTGREEN;
        }
        return Color.DIMGRAY;
    }

    private void appendLog(String message, Color color) {
        Text line = new Text(message + "\n");
        line.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        line.setFill(color);
        logFlow.getChildren().add(line);
    }
}
