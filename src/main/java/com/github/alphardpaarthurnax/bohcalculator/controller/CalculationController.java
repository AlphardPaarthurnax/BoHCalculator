package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.CalculationGoal;
import com.github.alphardpaarthurnax.bohcalculator.model.CalculationGoalType;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.CatalogItem;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftGoalProgress;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlacement;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanMissing;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanResult;
import com.github.alphardpaarthurnax.bohcalculator.model.CraftPlanStep;
import com.github.alphardpaarthurnax.bohcalculator.model.Thing;
import com.github.alphardpaarthurnax.bohcalculator.service.CalculationService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogImageService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogSelectionDialog;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CalculationController {
    @FXML private VBox goalRows;
    @FXML private VBox resultContent;
    @FXML private Button calculateButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;

    private final CalculationService service = CalculationService.getInstance();
    private final List<GoalEditor> goals = new ArrayList<>();

    @FXML
    public void initialize() {
        calculateButton.setDisable(true);
        progressIndicator.setVisible(false);
        showWelcome();
    }

    @FXML
    public void addCardGoal() {
        CatalogSelectionDialog.show(owner(), "选择 Cards 产物",
                        service.getCards(), item -> !hasGoal(CalculationGoalType.ELEMENT, item.getId()), comparator())
                .ifPresent(item -> addGoal(CalculationGoalType.ELEMENT, item));
    }

    @FXML
    public void addThingGoal() {
        CatalogSelectionDialog.show(owner(), "选择 Things 产物",
                        service.getThings(), item -> !hasGoal(CalculationGoalType.ELEMENT, item.getId()), comparator())
                .ifPresent(item -> addGoal(CalculationGoalType.ELEMENT, item));
    }

    @FXML
    public void addAspectGoal() {
        CatalogSelectionDialog.show(owner(), "选择 Aspect 需求",
                        service.getAspects(), item -> !hasGoal(CalculationGoalType.ASPECT, item.getId()), comparator())
                .ifPresent(item -> addGoal(CalculationGoalType.ASPECT, item));
    }

    @FXML
    public void calculate() {
        List<CalculationGoal> requests = goals.stream()
                .map(GoalEditor::toGoal)
                .toList();
        if (requests.isEmpty()) {
            return;
        }
        setCalculating(true);
        statusLabel.setText("正在展开配方并检查库存、工作台与槽位……");
        resultContent.getChildren().setAll(message("正在计算最优路径……", "#555555", 16));

        Task<CraftPlanResult> task = new Task<>() {
            @Override
            protected CraftPlanResult call() {
                return service.calculate(requests);
            }
        };
        task.setOnSucceeded(event -> {
            setCalculating(false);
            render(task.getValue());
        });
        task.setOnFailed(event -> {
            setCalculating(false);
            statusLabel.setText("计算失败");
            Throwable error = task.getException();
            resultContent.getChildren().setAll(message(
                    "计算失败：" + (error != null ? error.getMessage() : "未知错误"), "#b3261e", 15));
        });
        Thread worker = new Thread(task, "craft-path-planner");
        worker.setDaemon(true);
        worker.start();
    }

    private void addGoal(CalculationGoalType type, CatalogItem item) {
        GoalEditor editor = new GoalEditor(type, item);
        goals.add(editor);
        goalRows.getChildren().add(editor.row);
        calculateButton.setDisable(false);
        statusLabel.setText("已添加需求；调整数量后点击“计算最优路径”。");
    }

    private void removeGoal(GoalEditor editor) {
        goals.remove(editor);
        goalRows.getChildren().remove(editor.row);
        calculateButton.setDisable(goals.isEmpty());
        if (goals.isEmpty()) {
            showWelcome();
        }
    }

    private void render(CraftPlanResult result) {
        resultContent.getChildren().clear();
        String summary = result.complete()
                ? "可以完成全部需求，共需 " + result.steps().stream().filter(CraftPlanStep::executable).count() + " 步合成。"
                : "当前库存无法完全满足需求；下面给出最接近的路径和缺失内容。";
        statusLabel.setText(summary);
        resultContent.getChildren().add(sectionTitle(result.complete() ? "✓ 计算完成" : "△ 最接近的方案",
                result.complete() ? "#2e7d32" : "#b26a00"));

        VBox goalBox = panel("需求满足情况");
        for (CraftGoalProgress progress : result.goals()) {
            CatalogItem target = service.find(progress.goal().targetId());
            String suffix = "  " + Math.min(progress.achieved(), progress.goal().amount())
                    + " / " + progress.goal().amount();
            HBox row = itemLine(target, progress.goal().targetId(), 1, 40);
            Label amount = new Label(suffix);
            amount.setStyle("-fx-font-weight: bold; -fx-text-fill: "
                    + (progress.achieved() >= progress.goal().amount() ? "#2e7d32;" : "#b3261e;"));
            row.getChildren().add(amount);
            goalBox.getChildren().add(row);
            if (progress.goal().type() == CalculationGoalType.ASPECT && !progress.sources().isEmpty()) {
                HBox sources = new HBox(8);
                sources.setPadding(new Insets(0, 0, 4, 48));
                Label label = new Label("采用：");
                sources.getChildren().add(label);
                progress.sources().forEach((id, count) -> sources.getChildren().add(compactItem(id, count)));
                goalBox.getChildren().add(sources);
            }
        }
        resultContent.getChildren().add(goalBox);

        if (!result.steps().isEmpty()) {
            VBox steps = panel("合成路径");
            int number = 1;
            for (CraftPlanStep step : result.steps()) {
                steps.getChildren().add(renderStep(number++, step));
            }
            resultContent.getChildren().add(steps);
        }

        if (!result.missing().isEmpty()) {
            VBox missing = panel("缺失内容");
            missing.setStyle("-fx-background-color: #fff4f2; -fx-background-radius: 8; "
                    + "-fx-border-color: #e6aaa5; -fx-border-radius: 8;");
            for (CraftPlanMissing item : result.missing()) {
                CatalogItem target = service.find(item.targetId());
                HBox line = itemLine(target, item.targetId(), 1, 36);
                Label explanation = new Label("缺 " + Math.max(0, item.required() - item.available())
                        + "（需要 " + item.required() + "，当前可用 " + item.available() + "）— " + item.detail());
                explanation.setWrapText(true);
                explanation.setStyle("-fx-text-fill: #9b1c14;");
                HBox.setHgrow(explanation, Priority.ALWAYS);
                line.getChildren().add(explanation);
                missing.getChildren().add(line);
            }
            resultContent.getChildren().add(missing);
        }

        VBox notes = panel("计算说明");
        result.warnings().forEach(warning -> notes.getChildren().add(new Label("• " + warning)));
        resultContent.getChildren().add(notes);
    }

    private Node renderStep(int number, CraftPlanStep step) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle(step.executable()
                ? "-fx-background-color: #f6f8fa; -fx-background-radius: 7; -fx-border-color: #d7dde3; -fx-border-radius: 7;"
                : "-fx-background-color: #fff8e8; -fx-background-radius: 7; -fx-border-color: #e5bd68; -fx-border-radius: 7;");
        CatalogItem recipe = service.find(step.recipeId());
        Label title = new Label((step.executable() ? "步骤 " : "受阻步骤 ") + number + "："
                + displayName(recipe, step.recipeId()));
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        box.getChildren().add(title);

        if (step.workstationId() != null) {
            HBox workstation = new HBox(8, new Label("工作台："), compactItem(step.workstationId(), 1));
            workstation.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(workstation);
        }
        if (!step.placements().isEmpty()) {
            VBox inputs = new VBox(5, new Label("放入："));
            for (CraftPlacement placement : step.placements()) {
                HBox line = new HBox(8);
                line.setAlignment(Pos.CENTER_LEFT);
                Label slot = new Label(placement.slotLabel() + "：");
                slot.setMinWidth(80);
                line.getChildren().addAll(slot, compactItem(placement.elementId(), 1),
                        new Label(placement.consumed() ? "（消耗）" : "（保留）"));
                inputs.getChildren().add(line);
            }
            box.getChildren().add(inputs);
        }
        if (!step.effects().isEmpty()) {
            HBox outputs = new HBox(8, new Label("产物："));
            outputs.setAlignment(Pos.CENTER_LEFT);
            step.effects().forEach((id, amount) -> outputs.getChildren().add(compactItem(id, amount)));
            box.getChildren().add(outputs);
        }
        return box;
    }

    private VBox panel(String title) {
        VBox box = new VBox(9);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #d7dde3; -fx-border-radius: 8;");
        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        box.getChildren().addAll(heading, new Separator());
        return box;
    }

    private HBox compactItem(String id, int amount) {
        CatalogItem item = service.find(id);
        HBox chip = itemLine(item, id, amount, 28);
        chip.setPadding(new Insets(3, 7, 3, 4));
        chip.setStyle("-fx-background-color: #eef2f6; -fx-background-radius: 5;");
        return chip;
    }

    private HBox itemLine(CatalogItem item, String fallbackId, int amount, double imageSize) {
        ImageView image = new ImageView(CatalogImageService.imageFor(item));
        image.setFitWidth(imageSize);
        image.setFitHeight(imageSize);
        image.setPreserveRatio(true);
        String text = displayName(item, fallbackId) + (amount > 1 ? " × " + amount : "");
        Label label = new Label(text);
        label.setWrapText(true);
        HBox line = new HBox(8, image, label);
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    private String displayName(CatalogItem item, String fallback) {
        return item != null ? item.getDisplayName() : fallback;
    }

    private void setCalculating(boolean calculating) {
        progressIndicator.setVisible(calculating);
        calculateButton.setDisable(calculating || goals.isEmpty());
        goalRows.setDisable(calculating);
    }

    private boolean hasGoal(CalculationGoalType type, String id) {
        return goals.stream().anyMatch(goal -> goal.type == type && goal.item.getId().equals(id));
    }

    private javafx.stage.Window owner() {
        return goalRows.getScene() != null ? goalRows.getScene().getWindow() : null;
    }

    private <T extends CatalogItem> Comparator<T> comparator() {
        Collator collator = Collator.getInstance(Locale.CHINA);
        return (left, right) -> {
            int byName = collator.compare(left.getDisplayName(), right.getDisplayName());
            return byName != 0 ? byName : left.getId().compareTo(right.getId());
        };
    }

    private Label sectionTitle(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        return label;
    }

    private Label message(String text, String color, int size) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: " + size + "px; -fx-text-fill: " + color + ";");
        return label;
    }

    private void showWelcome() {
        statusLabel.setText("添加一个或多个产物/性相需求，然后开始计算。");
        resultContent.getChildren().setAll(
                sectionTitle("合成路径计算器", "#334155"),
                message("计算器会读取当前库存与已解锁工作台，递归展开中间配方，检查所有卡牌是否能同时放入工作台槽位。无法完成时仍会显示最接近的受阻路径和精确缺口。",
                        "#475569", 15));
    }

    private final class GoalEditor {
        private final CalculationGoalType type;
        private final CatalogItem item;
        private final Spinner<Integer> amount = new Spinner<>();
        private final HBox row = new HBox(9);

        private GoalEditor(CalculationGoalType type, CatalogItem item) {
            this.type = type;
            this.item = item;
            ImageView image = new ImageView(CatalogImageService.imageFor(item));
            image.setFitWidth(42);
            image.setFitHeight(42);
            image.setPreserveRatio(true);

            Label kind = new Label(type == CalculationGoalType.ASPECT ? "性相" : "产物");
            kind.setStyle("-fx-text-fill: #64748b;");
            Label name = new Label(item.getDisplayName());
            name.setWrapText(true);
            name.setStyle("-fx-font-weight: bold;");
            VBox labels = new VBox(2, kind, name);
            HBox.setHgrow(labels, Priority.ALWAYS);

            amount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    1, type == CalculationGoalType.ASPECT ? 999 : 99, 1));
            amount.setEditable(true);
            amount.setPrefWidth(82);
            Button remove = new Button("移除");
            remove.setOnAction(event -> removeGoal(this));

            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(7));
            row.setStyle("-fx-background-color: #f6f8fa; -fx-background-radius: 6;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(image, labels, spacer, new Label("数量"), amount, remove);
        }

        private CalculationGoal toGoal() {
            try {
                int typed = Integer.parseInt(amount.getEditor().getText().trim());
                int maximum = type == CalculationGoalType.ASPECT ? 999 : 99;
                amount.getValueFactory().setValue(Math.max(1, Math.min(maximum, typed)));
            } catch (NumberFormatException ignored) {
                amount.getEditor().setText(String.valueOf(amount.getValue()));
            }
            return new CalculationGoal(type, item.getId(), amount.getValue());
        }
    }
}
