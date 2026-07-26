package com.github.alphardpaarthurnax.bohcalculator.controller;

import com.github.alphardpaarthurnax.bohcalculator.craft.SkillAspectResolver;
import com.github.alphardpaarthurnax.bohcalculator.model.Aspect;
import com.github.alphardpaarthurnax.bohcalculator.model.Card;
import com.github.alphardpaarthurnax.bohcalculator.model.SkillConfiguration;
import com.github.alphardpaarthurnax.bohcalculator.service.AspectDataService;
import com.github.alphardpaarthurnax.bohcalculator.service.CardStockService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogImageService;
import com.github.alphardpaarthurnax.bohcalculator.utils.CatalogListCell;
import com.github.alphardpaarthurnax.bohcalculator.utils.QuantityStockSupport;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CardStockController extends QuantityStockSupport<Card> {
    private final CardStockService service = CardStockService.getInstance();
    private final Map<String, Aspect> aspects = new LinkedHashMap<>();

    @FXML private VBox skillPanel;
    @FXML private Spinner<Integer> skillLevelSpinner;
    @FXML private ComboBox<Aspect> wisdomChoice;
    @FXML private ComboBox<Aspect> attunementChoice;
    @FXML private Button cancelPresentationButton;
    @FXML private Button harmonizeButton;
    @FXML private Button cancelHarmonizeButton;
    @FXML private Label skillStateLabel;
    @FXML private FlowPane effectiveAspectsPane;

    private Card currentCard;
    private boolean updatingSkillControls;

    @FXML
    @Override
    public void initialize() {
        AspectDataService.getInstance().getAspects().forEach(aspect -> aspects.put(aspect.getId(), aspect));
        skillLevelSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9, 1));
        wisdomChoice.setCellFactory(ignored -> new CatalogListCell<>());
        wisdomChoice.setButtonCell(new CatalogListCell<>());
        attunementChoice.setCellFactory(ignored -> new CatalogListCell<>());
        attunementChoice.setButtonCell(new CatalogListCell<>());
        attunementChoice.getItems().setAll(SkillAspectResolver.ATTUNEMENT_IDS.stream()
                .map(aspects::get)
                .filter(java.util.Objects::nonNull)
                .toList());
        skillLevelSpinner.getEditor().setOnAction(event -> commitTypedLevel());
        skillLevelSpinner.getEditor().focusedProperty().addListener(
                (observable, wasFocused, isFocused) -> {
                    if (!isFocused) {
                        commitTypedLevel();
                    }
                });
        skillLevelSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!updatingSkillControls && currentCard != null && service.isSkill(currentCard) && newValue != null) {
                updateSkill(() -> service.setSkillLevel(currentCard.getId(), newValue), "等级已保存");
            }
        });
        super.initialize();
    }

    @Override protected ObservableList<Card> sourceItems() { return service.getCards(); }
    @Override protected int quantity(String id) { return service.getQuantity(id); }
    @Override protected void setQuantity(String id, int amount) { service.setQuantity(id, amount); }

    @Override
    protected void onItemShown(Card card) {
        currentCard = card;
        boolean skill = service.isSkill(card);
        skillPanel.setVisible(skill);
        skillPanel.setManaged(skill);
        if (!skill) {
            effectiveAspectsPane.getChildren().clear();
            return;
        }
        refreshSkillControls();
    }

    @FXML
    public void presentSkill() {
        if (currentCard == null) {
            return;
        }
        Aspect wisdom = wisdomChoice.getValue();
        Aspect attunement = attunementChoice.getValue();
        if (wisdom == null || attunement == null) {
            saveStatus.setText("请同时选择呈递的伟大之术和调和魂质");
            return;
        }
        updateSkill(() -> service.presentSkill(currentCard, wisdom.getId(), attunement.getId()), "呈递状态已保存");
    }

    @FXML
    public void cancelPresentation() {
        if (currentCard != null) {
            updateSkill(() -> service.cancelPresentation(currentCard.getId()), "已取消呈递和调和");
        }
    }

    @FXML
    public void harmonizeSkill() {
        if (currentCard != null) {
            updateSkill(() -> service.setHarmonized(currentCard.getId(), true), "Skill 已标记为调和完毕");
        }
    }

    @FXML
    public void cancelHarmonization() {
        if (currentCard != null) {
            updateSkill(() -> service.setHarmonized(currentCard.getId(), false), "已取消调和完毕状态");
        }
    }

    private void updateSkill(Runnable update, String message) {
        try {
            update.run();
            refreshSkillControls();
            saveStatus.setText(message);
        } catch (RuntimeException exception) {
            saveStatus.setText("保存失败：" + exception.getMessage());
            refreshSkillControls();
        }
    }

    private void commitTypedLevel() {
        if (updatingSkillControls || currentCard == null || !service.isSkill(currentCard)) {
            return;
        }
        try {
            int typed = Integer.parseInt(skillLevelSpinner.getEditor().getText().trim());
            skillLevelSpinner.getValueFactory().setValue(Math.max(1, Math.min(9, typed)));
        } catch (NumberFormatException ignored) {
            skillLevelSpinner.getEditor().setText(String.valueOf(skillLevelSpinner.getValue()));
        }
    }

    private void refreshSkillControls() {
        if (currentCard == null || !service.isSkill(currentCard)) {
            return;
        }
        SkillConfiguration configuration = service.getSkillConfiguration(currentCard.getId());
        updatingSkillControls = true;
        try {
            skillLevelSpinner.getValueFactory().setValue(configuration.level());
            wisdomChoice.getItems().setAll(service.getInitialWisdoms(currentCard).stream()
                    .map(aspects::get)
                    .filter(java.util.Objects::nonNull)
                    .toList());
            wisdomChoice.setValue(aspects.get(configuration.wisdomId()));
            attunementChoice.setValue(aspects.get(configuration.attunementId()));
        } finally {
            updatingSkillControls = false;
        }

        cancelPresentationButton.setDisable(!configuration.presented());
        harmonizeButton.setDisable(!configuration.presented() || configuration.harmonized());
        cancelHarmonizeButton.setDisable(!configuration.harmonized());
        skillStateLabel.setText(stateDescription(configuration));
        renderEffectiveAspects(service.getEffectiveAspects(currentCard));
    }

    private String stateDescription(SkillConfiguration configuration) {
        if (!configuration.presented()) {
            return "等级 " + configuration.level() + "；尚未呈递，保留原有两种伟大之术。";
        }
        Aspect wisdom = aspects.get(configuration.wisdomId());
        Aspect attunement = aspects.get(configuration.attunementId());
        String wisdomName = wisdom != null ? wisdom.getDisplayName() : configuration.wisdomId();
        String attunementName = attunement != null ? attunement.getDisplayName() : configuration.attunementId();
        return "等级 " + configuration.level() + "；已呈递至“" + wisdomName + "”；"
                + (configuration.harmonized() ? "调和完毕（原调和魂质：" + attunementName + "）。"
                : "当前带有“" + attunementName + "”。");
    }

    private void renderEffectiveAspects(Map<String, Integer> values) {
        effectiveAspectsPane.getChildren().clear();
        values.forEach((id, amount) -> {
            Aspect aspect = aspects.get(id);
            if (aspect == null || amount == null || amount <= 0) {
                return;
            }
            ImageView image = new ImageView(CatalogImageService.imageFor(aspect));
            image.setFitWidth(24);
            image.setFitHeight(24);
            image.setPreserveRatio(true);
            Label text = new Label(aspect.getDisplayName() + (amount == 1 ? "" : " × " + amount));
            HBox chip = new HBox(6, image, text);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setPadding(new Insets(4, 7, 4, 5));
            chip.setStyle("-fx-background-color: #e8edf3; -fx-background-radius: 5;");
            effectiveAspectsPane.getChildren().add(chip);
        });
    }
}
