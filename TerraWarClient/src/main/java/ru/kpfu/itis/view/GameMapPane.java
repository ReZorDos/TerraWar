package ru.kpfu.itis.view;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
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
    private final GameMap gameMap;
    private final Pane mapPane;
    private final Text turnInfoText;
    private final Text unitCountText;
    private final Button endTurnButton;
    private final Button buyUnitButton;
    private final Button buyFarmButton;
    private final Button buyTowerButton;
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
        this.buyFarmButton = new Button("Купить ферму");
        this.buyUnitButton = new Button("Купить юнит");
        this.buyTowerButton = new Button("Купить башню");
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
        turnInfoText = new Text();
        unitCountText = new Text();
        endTurnButton = new Button("Завершить ход");
        initializeUI();
        initializeMap();
        setupEventHandlers();
        updateTurnInfo();
        turnManager.startPlayerTurn();
    }

    private void initializeUI() {
        HBox controlPanel = new HBox(20);
        controlPanel.setAlignment(Pos.TOP_LEFT);
        controlPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");
        controlPanel.getChildren().addAll(turnInfoText, unitCountText, buyUnitButton,
                buyFarmButton, buyTowerButton, endTurnButton);
        mapPane.setPrefSize(900, 550);
        turnInfoText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        unitCountText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        this.getChildren().addAll(controlPanel, mapPane);
    }

    private void initializeMap() {
        mapRenderer.initializeMap();
    }

    private void updateTurnInfo() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            String turnInfo = String.format("Игрок: %s | Деньги: %d | Доход: %d",
                    currentPlayer.getName(),
                    currentPlayer.getMoney(),
                    currentPlayer.getIncome());
            turnInfoText.setText(turnInfo);
            turnInfoText.setFill(getPlayerTextColor(currentPlayer.getId()));
            int unitCount = unitManager.getPlayerUnits(currentPlayer.getId()).size();
            unitCountText.setText("Юниты: " + unitCount + " | Фермы: " + currentPlayer.getFarms().size());
            unitCountText.setFill(getPlayerTextColor(currentPlayer.getId()));
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
        buyUnitButton.setOnAction(event -> placementController.handleBuyUnit());
        buyFarmButton.setOnAction(event -> placementController.handleBuyFarm());
        buyTowerButton.setOnAction(event -> placementController.handleBuyTower());
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

        // ✅ ИСПРАВЛЕНИЕ: Трогаем только существующие гексы (не null)
        mapRenderer.getHexagons().values().forEach(hexagon -> {
            Hex hexData = gameMap.getHex(hexagon.getGridX(), hexagon.getGridY());
            if (hexData != null) {  // Только для существующих гексов
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