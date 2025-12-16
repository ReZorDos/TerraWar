package ru.kpfu.itis.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Hex;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.model.Unit;
import ru.kpfu.itis.service.*;
import ru.kpfu.itis.state.GameState;
import java.util.List;

public class GameMapPane extends VBox {

    private final Game game;
    private final GameState gameState;
    private final GameActionService gameActionService;
    private final GameTurnManager turnManager;
    private final UnitManager unitManager;
    private final TowerManager towerManager;
    private final GameMap gameMap;
    private final Pane mapPane;
    private final Label playerNameLabel;
    private final Label moneyLabel;
    private final Label incomeLabel;
    private final Label unitsLabel;
    private final Label farmsLabel;
    private final Label towersLabel;
    private final Button endTurnButton;
    private final MapRenderer mapRenderer;
    private final PlacementController placementController;
    private final ImageCache imageCache;
    private Unit selectedUnit = null;
    private List<Hex> actionHexes = null;

    public GameMapPane(GameMap gameMap,
                       GameActionService gameActionService, PlayerService playerService,
                       Game game,
                       GameTurnManager turnManager, UnitManager unitManager,
                       UnitShop unitShop,
                       GameMapService gameMapService,
                       FarmManager farmManager,
                       FarmShop farmShop,
                       TowerManager towerManager,
                       TowerShop towerShop) {
        this.gameMap = gameMap;
        this.gameActionService = gameActionService;
        this.game = game;
        this.turnManager = turnManager;
        this.unitManager = unitManager;
        this.towerManager = towerManager;
        this.gameState = new GameState();
        this.mapPane = new Pane();
        this.imageCache = new ImageCache();
        this.mapRenderer = new MapRenderer(
                gameMap,
                game,
                unitManager,
                farmManager,
                towerManager,
                mapPane,
                imageCache
        );
        this.placementController = new PlacementController(
                gameMap,
                game,
                gameActionService,
                gameMapService,
                unitManager,
                unitShop,
                farmManager,
                farmShop,
                towerManager,
                towerShop,
                mapRenderer,
                imageCache,
                new PlacementController.UiCallbacks() {
                    @Override
                    public void refreshMap() {
                        initializeMap();
                    }

                    @Override
                    public void refreshTurnInfo() {
                        updateTurnInfo();
                    }

                    @Override
                    public void refreshHighlights() {
                        GameMapPane.this.refreshHighlights();
                    }

                    @Override
                    public void showAlert(String title, String message) {
                        GameMapPane.this.showAlert(title, message);
                    }
                }
        );
        gameState.setCurrentPlayerId(game.getCurrentPlayer().getId());
        playerNameLabel = new Label();
        moneyLabel = new Label();
        incomeLabel = new Label();
        unitsLabel = new Label();
        farmsLabel = new Label();
        towersLabel = new Label();
        endTurnButton = new Button("Завершить ход");
        initializeUI();
        initializeMap();
        setupEventHandlers();
        updateTurnInfo();
        turnManager.startPlayerTurn();
    }

    private void initializeUI() {
        HBox controlPanel = new HBox(0);
        controlPanel.setAlignment(Pos.CENTER_LEFT);
        controlPanel.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #2c3e50, #34495e); " +
                "-fx-padding: 12px 20px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
        );
        controlPanel.setPrefHeight(90);

        VBox playerInfoBox = createPlayerInfoSection();
        Region separator1 = createSeparator();
        HBox shopSection = createShopSection();
        Region separator2 = createSeparator();
        VBox statsSection = createStatsSection();

        HBox.setHgrow(shopSection, Priority.ALWAYS);
        controlPanel.getChildren().addAll(playerInfoBox, separator1, shopSection, separator2, statsSection);

        mapPane.setPrefSize(1100, 700);
        mapPane.setStyle("-fx-background-color: #2b2b2b;");
        this.setStyle("-fx-background-color: #2b2b2b;");
        this.getChildren().addAll(controlPanel, mapPane);
    }

    private VBox createPlayerInfoSection() {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 20, 0, 0));

        playerNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        playerNameLabel.setStyle("-fx-text-fill: white;");

        HBox moneyBox = new HBox(8);
        moneyBox.setAlignment(Pos.CENTER_LEFT);
        Label moneyIcon = createIconLabel("💰", 16);
        moneyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        moneyLabel.setStyle("-fx-text-fill: #f39c12;");
        moneyBox.getChildren().addAll(moneyIcon, moneyLabel);

        HBox incomeBox = new HBox(8);
        incomeBox.setAlignment(Pos.CENTER_LEFT);
        Label incomeIcon = createIconLabel("📈", 16);
        incomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        incomeLabel.setStyle("-fx-text-fill: #27ae60;");
        incomeBox.getChildren().addAll(incomeIcon, incomeLabel);

        box.getChildren().addAll(playerNameLabel, moneyBox, incomeBox);
        return box;
    }

    private HBox createShopSection() {
        HBox shopBox = new HBox(12);
        shopBox.setAlignment(Pos.CENTER);
        shopBox.setPadding(new Insets(0, 20, 0, 20));

        shopBox.getChildren().addAll(
                createShopButton("unit_1", "Юнит", () -> placementController.handleBuyUnit()),
                createShopButton("farm", "Ферма", () -> placementController.handleBuyFarm()),
                createShopButton("tower_1", "Башня", () -> placementController.handleBuyTower())
        );

        return shopBox;
    }

    private VBox createStatsSection() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(0, 0, 0, 20));

        HBox statsRow = new HBox(15);
        statsRow.setAlignment(Pos.CENTER_RIGHT);

        String statsStyle = "-fx-text-fill: #ecf0f1;";
        unitsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        unitsLabel.setStyle(statsStyle);
        farmsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        farmsLabel.setStyle(statsStyle);
        towersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        towersLabel.setStyle(statsStyle);

        statsRow.getChildren().addAll(unitsLabel, farmsLabel, towersLabel);

        endTurnButton.setPrefWidth(140);
        endTurnButton.setPrefHeight(35);
        applyEndTurnButtonStyle(endTurnButton, false);
        endTurnButton.setOnMouseEntered(e -> applyEndTurnButtonStyle(endTurnButton, true));
        endTurnButton.setOnMouseExited(e -> applyEndTurnButtonStyle(endTurnButton, false));

        box.getChildren().addAll(statsRow, endTurnButton);
        return box;
    }

    private Button createShopButton(String imageKey, String name, Runnable onClick) {
        Button button = new Button();
        button.setPrefSize(80, 70);
        button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #34495e, #2c3e50); " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #7f8c8d; " +
                "-fx-border-width: 1.5; " +
                "-fx-border-radius: 8; " +
                "-fx-padding: 5;"
        );

        VBox content = new VBox(4);
        content.setAlignment(Pos.CENTER);

        Image iconImage = imageCache.get(imageKey);
        if (iconImage != null) {
            ImageView imageView = new ImageView(iconImage);
            imageView.setFitWidth(40);
            imageView.setFitHeight(40);
            imageView.setPreserveRatio(true);
            content.getChildren().add(imageView);
        } else {
            Label iconText = new Label(name.substring(0, 1));
            iconText.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            iconText.setStyle("-fx-text-fill: white;");
            content.getChildren().add(iconText);
        }

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        nameLabel.setStyle("-fx-text-fill: #ecf0f1;");
        content.getChildren().add(nameLabel);

        button.setGraphic(content);
        button.setOnAction(e -> onClick.run());

        button.setOnMouseEntered(e -> applyShopButtonStyle(button, true));
        button.setOnMouseExited(e -> applyShopButtonStyle(button, false));

        return button;
    }

    private Region createSeparator() {
        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setStyle("-fx-background-color: rgba(255,255,255,0.2);");
        return separator;
    }

    private Label createIconLabel(String emoji, double fontSize) {
        Label label = new Label(emoji);
        label.setFont(Font.font(fontSize));
        return label;
    }


    private void initializeMap() {
        mapRenderer.initializeMap();
    }

    private void updateTurnInfo() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        playerNameLabel.setText(currentPlayer.getName());
        Color playerColor = getPlayerTextColor(currentPlayer.getId());
        playerNameLabel.setStyle("-fx-text-fill: " + colorToHex(playerColor) + ";");

        moneyLabel.setText(String.valueOf(currentPlayer.getMoney()));

        int income = currentPlayer.getIncome();
        incomeLabel.setText(income >= 0 ? "+" + income : String.valueOf(income));

        int unitCount = unitManager.getPlayerUnits(currentPlayer.getId()).size();
        int farmCount = currentPlayer.getFarms().size();
        int towerCount = towerManager.getPlayerTowers(currentPlayer.getId()).size();

        unitsLabel.setText("⚔ " + unitCount);
        farmsLabel.setText("🌾 " + farmCount);
        towersLabel.setText("🏰 " + towerCount);
    }

    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void applyShopButtonStyle(Button button, boolean hover) {
        if (hover) {
            button.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); " +
                    "-fx-background-radius: 8; " +
                    "-fx-border-color: #5dade2; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-padding: 5; " +
                    "-fx-effect: dropshadow(gaussian, rgba(52,152,219,0.5), 5, 0, 0, 2);"
            );
        } else {
            button.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #34495e, #2c3e50); " +
                    "-fx-background-radius: 8; " +
                    "-fx-border-color: #7f8c8d; " +
                    "-fx-border-width: 1.5; " +
                    "-fx-border-radius: 8; " +
                    "-fx-padding: 5;"
            );
        }
    }

    private void applyEndTurnButtonStyle(Button button, boolean hover) {
        String baseStyle = "-fx-text-fill: white; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 6; ";
        if (hover) {
            button.setStyle(baseStyle +
                    "-fx-background-color: linear-gradient(to bottom, #ec7063, #e74c3c); " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 4, 0, 0, 2);"
            );
        } else {
            button.setStyle(baseStyle +
                    "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b); " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 3, 0, 0, 2);"
            );
        }
    }

    private Color getPlayerTextColor(int playerId) {
        return switch (playerId) {
            case 0 -> Color.DARKRED;
            case 1 -> Color.DARKBLUE;
            default -> Color.BLACK;
        };
    }

    private void setupEventHandlers() {
        mapPane.setOnMouseClicked(event -> {
            TexturedHexagon clickedHex = getHexAtPixel(event.getX(), event.getY());
            if (clickedHex != null) {
                handleHexClick(clickedHex);
            }
        });
        endTurnButton.setOnAction(event -> endTurn());
    }

    private void handleHexClick(TexturedHexagon clickedHex) {
        if (placementController.isPlacementActive()) {
            placementController.handleHexClick(clickedHex);
            return;
        }

        Unit unitOnHex = unitManager.getUnitAt(clickedHex.getGridX(), clickedHex.getGridY());
        if (unitOnHex != null &&
                unitOnHex.getOwnerId() == gameState.getCurrentPlayerId() &&
                unitOnHex.canAct()) {
            if (selectedUnit != unitOnHex) {
                selectUnit(unitOnHex);
            } else {
                deselectUnit();
            }
            return;
        }

        if (selectedUnit != null && actionHexes != null &&
                gameActionService.isHexInRadius(actionHexes, clickedHex.getGridX(), clickedHex.getGridY())) {
            boolean success = gameActionService.actWithUnit(
                    selectedUnit,
                    clickedHex.getGridX(),
                    clickedHex.getGridY()
            );
            if (success) {
                deselectUnit();
                updateTurnInfo();
                initializeMap();
            }
            return;
        }

        deselectUnit();
    }

    private void selectUnit(Unit unit) {
        selectedUnit = unit;
        TexturedHexagon hexagon = mapRenderer.getHexagonAt(unit.getHexX(), unit.getHexY());
        if (hexagon != null) {
            hexagon.setSelected(true);
        }

        actionHexes = gameActionService.calculateActionRadius(unit);
        refreshHighlights();
    }

    private void deselectUnit() {
        if (selectedUnit != null) {
            TexturedHexagon hexagon = mapRenderer.getHexagonAt(selectedUnit.getHexX(), selectedUnit.getHexY());
            if (hexagon != null) {
                hexagon.setSelected(false);
            }
        }

        selectedUnit = null;
        actionHexes = null;
        if (!placementController.isPlacementActive()) {
            refreshHighlights();
        }
    }

    private void refreshHighlights() {
        if (placementController.isPlacementActive()) {
            return;
        }

        mapRenderer.getHexagons().values().forEach(hexagon -> {
            Hex hexData = gameMap.getHex(hexagon.getGridX(), hexagon.getGridY());
            if (hexData != null) {
                hexagon.setHighlighted(false);
                hexagon.setSelected(false);
            }
        });

        if (selectedUnit != null && actionHexes != null) {
            for (Hex hex : actionHexes) {
                TexturedHexagon hexagon = mapRenderer.getHexagonAt(hex.getX(), hex.getY());
                if (hexagon != null) {
                    hexagon.setHighlighted(true);
                }
            }

            TexturedHexagon unitHex = mapRenderer.getHexagonAt(selectedUnit.getHexX(), selectedUnit.getHexY());
            if (unitHex != null) {
                unitHex.setSelected(true);
            }
        }
    }

    private void endTurn() {
        placementController.cancelPlacementIfActive();
        deselectUnit();
        turnManager.endPlayerTurn();
        turnManager.startPlayerTurn();
        updateCurrentPlayer();
        refreshHighlights();
        updateTurnInfo();
        initializeMap();
    }

    private void updateCurrentPlayer() {
        if (game.getCurrentPlayer() != null) {
            gameState.setCurrentPlayerId(game.getCurrentPlayer().getId());
        }
    }

    public TexturedHexagon getHexAtPixel(double mouseX, double mouseY) {
        return mapRenderer.getHexAtPixel(mouseX, mouseY);
    }

    public TexturedHexagon getHexagonAt(int gridX, int gridY) {
        return mapRenderer.getHexagonAt(gridX, gridY);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
