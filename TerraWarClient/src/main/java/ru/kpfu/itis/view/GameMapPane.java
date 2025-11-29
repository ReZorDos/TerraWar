package ru.kpfu.itis.view;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
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

import java.util.*;

public class GameMapPane extends VBox {

    private final GameMap gameMap;
    private final Game game;
    private final GameState gameState;

    private final GameActionService gameActionService;
    private final GameTurnManager turnManager;
    private final UnitManager unitManager;
    private final UnitShop unitShop;

    private final Map<String, Hexagon> hexagons;
    private final Pane mapPane;
    private final Text turnInfoText;
    private final Text unitCountText;
    private final Button endTurnButton;
    private final Button buyUnitButton;

    private Unit selectedUnit = null;
    private List<Hex> actionHexes = null;
    private Integer placementLevel = null;
    private Integer placementPrice = null;

    public GameMapPane(GameMap gameMap,
                       GameActionService gameActionService, Game game,
                       GameTurnManager turnManager, UnitManager unitManager,
                       UnitShop unitShop) {
        this.gameMap = gameMap;
        this.gameActionService = gameActionService;
        this.game = game;
        this.turnManager = turnManager;
        this.unitManager = unitManager;
        this.unitShop = unitShop;
        this.buyUnitButton = new Button("Купить юнит");
        this.gameState = new GameState();
        this.hexagons = new HashMap<>();
        this.mapPane = new Pane();

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

        controlPanel.getChildren().addAll(turnInfoText, unitCountText, buyUnitButton, endTurnButton);

        mapPane.setPrefSize(900, 550);
        turnInfoText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        unitCountText.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        this.getChildren().addAll(controlPanel, mapPane);
    }

    private void initializeMap() {
        mapPane.getChildren().clear();
        hexagons.clear();

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hexagon hexagon = new Hexagon(x, y);
                hexagon.positionAtGridCoords();

                Hex hexData = gameMap.getHex(x, y);
                updateHexagonAppearance(hexagon, hexData);

                String key = x + "," + y;
                hexagons.put(key, hexagon);
                mapPane.getChildren().add(hexagon);
            }
        }

        drawUnits();
    }

    private void drawUnits() {
        mapPane.getChildren().removeIf(node -> node instanceof Text);
        List<Unit> allUnits = unitManager.getAllUnits();

        for (Unit unit : allUnits) {
            Hexagon hexagon = getHexagonAt(unit.getHexX(), unit.getHexY());
            if (hexagon != null) {
                drawUnitNumber(hexagon, unit);
            }
        }
    }

    private void drawUnitNumber(Hexagon hexagon, Unit unit) {
        Text unitText = new Text(String.valueOf(unit.getLevel()));
        unitText.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        switch (unit.getOwnerId()) {
            case 0 -> unitText.setFill(Color.DARKRED);
            case 1 -> unitText.setFill(Color.DARKBLUE);
            default -> unitText.setFill(Color.BLACK);
        }

        double[] center = Hexagon.getCenterCoords(hexagon.getGridX(), hexagon.getGridY());
        unitText.setX(center[0] - 5);
        unitText.setY(center[1] + 7);
        mapPane.getChildren().add(unitText);
    }

    private void updateTurnInfo() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            turnInfoText.setText(turnManager.getTurnInfo());
            turnInfoText.setFill(getPlayerTextColor(currentPlayer.getId()));

            int unitCount = unitManager.getPlayerUnits(currentPlayer.getId()).size();
            unitCountText.setText("Юниты: " + unitCount);
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

    private void updateHexagonAppearance(Hexagon hexagon, Hex hexData) {
        hexagon.setColor(getOwnerColor(hexData.getOwnerId()));
    }

    private Color getOwnerColor(int ownerId) {
        if (ownerId == -1) {
            return Color.LIGHTGRAY;
        }
        return switch (ownerId) {
            case 0 -> Color.RED;
            case 1 -> Color.BLUE;
            default -> Color.ORANGE;
        };
    }

    private void setupEventHandlers() {
        mapPane.setOnMouseClicked(event -> {
            Hexagon clickedHex = getHexAtPixel(event.getX(), event.getY());
            if (clickedHex != null) {
                handleHexClick(clickedHex);
            }
        });

        endTurnButton.setOnAction(event -> endTurn());
        buyUnitButton.setOnAction(event -> handleBuyUnit());
    }

    private void handleHexClick(Hexagon clickedHex) {
        // Если активен режим размещения юнита
        if (placementLevel != null) {
            handleUnitPlacement(clickedHex);
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

    private void handleUnitPlacement(Hexagon clickedHex) {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        int x = clickedHex.getGridX();
        int y = clickedHex.getGridY();

        Hex hex = gameMap.getHex(x, y);

        if (hex == null || hex.getOwnerId() != currentPlayer.getId()) {
            showAlert("Ошибка", "Можно размещать юнитов только на своей территории!");
            return;
        }

        if (unitManager.getUnitAt(x, y) != null) {
            showAlert("Ошибка", "На этом гексе уже есть юнит!");
            return;
        }

        // Создаем юнит
        Unit newUnit = unitShop. purchaseUnit(unitManager, currentPlayer.getId(), x, y, placementLevel);

        // Списание денег
        currentPlayer.setMoney(currentPlayer.getMoney() - placementPrice);

        // Выключаем режим размещения
        disableUnitPlacementMode();

        // Обновляем интерфейс
        updateTurnInfo();
        initializeMap();

        showAlert("Успех", "Юнит уровня " + placementLevel + " размещен на гексе (" + x + "," + y + ")");
    }


    private void selectUnit(Unit unit) {
        selectedUnit = unit;

        Hexagon hexagon = getHexagonAt(unit.getHexX(), unit.getHexY());
        if (hexagon != null) {
            hexagon.setSelected(true);
        }

        actionHexes = gameActionService.calculateActionRadius(unit);
        refreshHighlights();

    }

    private void deselectUnit() {
        if (selectedUnit != null) {
            Hexagon hexagon = getHexagonAt(selectedUnit.getHexX(), selectedUnit.getHexY());
            if (hexagon != null) {
                hexagon.setSelected(false);
            }
        }

        selectedUnit = null;
        actionHexes = null;

        // Обновляем подсветку только если не в режиме размещения
        if (placementLevel == null) {
            refreshHighlights();
        }
    }

    private void refreshHighlights() {
        // Не обновляем подсветку если активен режим размещения
        if (placementLevel != null) {
            return;
        }

        hexagons.values().forEach(hexagon -> {
            hexagon.setHighlighted(false);
            hexagon.setSelected(false);
        });

        if (selectedUnit != null && actionHexes != null) {
            for (Hex hex : actionHexes) {
                Hexagon hexagon = getHexagonAt(hex.getX(), hex.getY());
                if (hexagon != null) {
                    hexagon.setHighlighted(true);
                }
            }

            Hexagon unitHex = getHexagonAt(selectedUnit.getHexX(), selectedUnit.getHexY());
            if (unitHex != null) {
                unitHex.setSelected(true);
            }
        }
    }

    private void endTurn() {
        if (placementLevel != null) {
            disableUnitPlacementMode();
        }

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

    public Hexagon getHexAtPixel(double mouseX, double mouseY) {
        return hexagons.values().stream()
                .filter(hexagon -> hexagon.getBoundsInParent().contains(mouseX, mouseY))
                .findFirst()
                .orElse(null);
    }

    public Hexagon getHexagonAt(int gridX, int gridY) {
        String key = gridX + "," + gridY;
        return hexagons.get(key);
    }

    private void handleBuyUnit() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        // Диалог выбора уровня юнита
        List<Integer> levels = Arrays.asList(1, 2, 3);
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(1, levels);
        dialog.setTitle("Покупка юнита");

        // Создаем информационный заголовок с ценами
        StringBuilder header = new StringBuilder();
        header.append("Выберите уровень юнита\n");
        header.append("Ваши деньги: ").append(currentPlayer.getMoney()).append(" монет\n\n");
        header.append("Стоимость юнитов:\n");
        for (int level : levels) {
            int price = unitShop.getUnitPrice(level);
            header.append("Уровень ").append(level).append(": ").append(price)
                    .append(" монет ").append("\n");
        }

        dialog.setHeaderText(header.toString());
        dialog.setContentText("Уровень:");

        Optional<Integer> result = dialog.showAndWait();
        if (result.isPresent()) {
            int level = result.get();
            int price = unitShop.getUnitPrice(level);

            if (!unitShop.canAffordUnit(currentPlayer.getMoney(), level)) {
                showAlert("Недостаточно денег",
                                "Юнит уровня " + level + " стоит " + price + " монет\n");
                return;
            }

            // Включаем режим размещения юнита
            enableUnitPlacementMode(level, price);
        }
    }

    private void enableUnitPlacementMode(int level, int price) {
        placementLevel = level;
        placementPrice = price;

        // Подсвечиваем доступные для размещения гексы
        highlightAvailableHexesForPlacement();

        showAlert("Размещение юнита", "Выберите гекс на своей территории для размещения юнита уровня " + level);
    }

    private void disableUnitPlacementMode() {
        placementLevel = null;
        placementPrice = null;
        clearPlacementHighlights();
    }

    private void highlightAvailableHexesForPlacement() {
        clearPlacementHighlights();

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hex hex = gameMap.getHex(x, y);
                if (hex != null && hex.getOwnerId() == currentPlayer.getId()) {
                    // Проверяем, что на hex нет других юнитов
                    if (unitManager.getUnitAt(x, y) == null) {
                        Hexagon hexagon = getHexagonAt(x, y);
                        if (hexagon != null) {
                            hexagon.setHighlighted(true);
                            hexagon.setStroke(Color.PURPLE);
                            hexagon.setStrokeWidth(2.0);
                        }
                    }
                }
            }
        }
    }

    private void clearPlacementHighlights() {
        hexagons.values().forEach(hexagon -> {
            if (hexagon.isHighlighted() && hexagon.getStroke() == Color.PURPLE) {
                hexagon.setHighlighted(false);
                hexagon.setStroke(Color.BLACK);
                hexagon.setStrokeWidth(1.0);
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}