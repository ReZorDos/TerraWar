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
import ru.kpfu.itis.enums.PlacementMode;
import ru.kpfu.itis.model.*;
import ru.kpfu.itis.service.*;
import ru.kpfu.itis.state.GameState;

import java.util.*;

public class GameMapPane extends VBox {

    private final GameMap gameMap;
    private final Game game;
    private final GameState gameState;
    private final GameMapService gameMapService;

    private final GameActionService gameActionService;
    private final PlayerService playerService;
    private final GameTurnManager turnManager;
    private final UnitManager unitManager;
    private final UnitShop unitShop;

    private final Map<String, Hexagon> hexagons;
    private final Pane mapPane;
    private final Text turnInfoText;
    private final Text unitCountText;
    private final Button endTurnButton;
    private final Button buyUnitButton;

    private final FarmManager farmManager;
    private final FarmShop farmShop;
    private final Button buyFarmButton;

    private PlacementMode placementMode = PlacementMode.NONE;

    private Unit selectedUnit = null;
    private List<Hex> actionHexes = null;
    private Integer placementLevel = null;
    private Integer placementPrice = null;

    public GameMapPane(GameMap gameMap,
                       GameActionService gameActionService, PlayerService playerService,
                       Game game,
                       GameTurnManager turnManager, UnitManager unitManager,
                       UnitShop unitShop,
                       GameMapService gameMapService,
                       FarmManager farmManager,
                       FarmShop farmShop) {
        this.gameMap = gameMap;
        this.gameActionService = gameActionService;
        this.playerService = playerService;
        this.game = game;
        this.turnManager = turnManager;
        this.unitManager = unitManager;
        this.unitShop = unitShop;
        this.gameMapService = gameMapService;
        this.farmManager = farmManager;
        this.farmShop = farmShop;
        this.buyFarmButton = new Button("Купить ферму");
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

        controlPanel.getChildren().addAll(turnInfoText, unitCountText, buyUnitButton, buyFarmButton, endTurnButton);

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

                Farm farm = farmManager.getFarmAt(x, y);
                if (farm != null) {
                    hexagon.setStroke(Color.DARKGREEN);
                    hexagon.setStrokeWidth(3.0);
                }

                String key = x + "," + y;
                hexagons.put(key, hexagon);
                mapPane.getChildren().add(hexagon);
            }
        }

        drawUnits();
        drawFarms();
    }

    private void drawUnits() {
        // Удаляем только тексты юнитов (цифры), но не тексты ферм (букву "Ф")
        mapPane.getChildren().removeIf(node ->
                node instanceof Text && !((Text) node).getText().equals("Ф")
        );

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
        buyFarmButton.setOnAction(event -> handleBuyFarm());
    }

    private void handleHexClick(Hexagon clickedHex) {
        if (placementMode == PlacementMode.FARM) {
            handleFarmPlacement(clickedHex);
            return;
        }

        if (placementMode == PlacementMode.UNIT) {
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

        if (!canPlaceUnitOnHex(currentPlayer.getId(), x, y)) {
            showAlert("Ошибка",
                    "Можно размещать юнитов только на своей территории ИЛИ на соседних с вашей базой гексах!");
            return;
        }

        Farm farmOnHex = farmManager.getFarmAt(x, y);
        if (farmOnHex != null) {
            return;
        }

        Unit existingUnit = unitManager.getUnitAt(x, y);
        if (existingUnit != null) {
            if (existingUnit.getOwnerId() != currentPlayer.getId()) {
                Unit tempUnitForCheck = new Unit(-1, currentPlayer.getId(), x, y, placementLevel);

                if (tempUnitForCheck.canDefeat(existingUnit)) {
                    unitManager.removeUnit(existingUnit.getId());
                } else {
                    showAlert("Ошибка",
                            "Нельзя разместить юнита уровня " + placementLevel +
                                    " на вражеского юнита уровня " + existingUnit.getLevel() + "!");
                    return;
                }
            } else {
                // Дружественный юнит - нельзя размещать
                return;
            }
        }

        boolean canActThisTurn = (hex.getOwnerId() == currentPlayer.getId());
        Unit tempUnit = new Unit(-1, currentPlayer.getId(), x, y, placementLevel);

        gameActionService.captureTerritory(tempUnit, hex);

        Unit newUnit = unitShop.purchaseUnit(unitManager, currentPlayer.getId(), x, y, placementLevel);
        newUnit.setHasActed(!canActThisTurn);

        currentPlayer.setMoney(currentPlayer.getMoney() - placementPrice);

        disableUnitPlacementMode();

        updateTurnInfo();
        initializeMap();
    }

    private boolean canPlaceUnitOnHex(int playerId, int hexX, int hexY) {
        Hex targetHex = gameMap.getHex(hexX, hexY);
        if (targetHex == null) return false;

        Farm farmOnHex = farmManager.getFarmAt(hexX, hexY);
        if (farmOnHex != null) {
            return false;
        }

        if (targetHex.getOwnerId() == playerId) {
            return true;
        }

        List<Hex> baseHexes = getPlayerBaseHexes(playerId);

        for (Hex baseHex : baseHexes) {
            List<Hex> neighbors = gameMapService.getNeighbors(baseHex.getX(), baseHex.getY());
            for (Hex neighbor : neighbors) {
                if (neighbor.getX() == hexX && neighbor.getY() == hexY) {
                    Unit existingUnit = unitManager.getUnitAt(hexX, hexY);
                    if (existingUnit != null && existingUnit.getOwnerId() != playerId) {
                        Unit tempUnitForCheck = new Unit(-1, playerId, hexX, hexY, placementLevel);
                        return tempUnitForCheck.canDefeat(existingUnit);
                    }
                    return true;
                }
            }
        }

        return false;
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

        if (placementMode == PlacementMode.NONE) {
            refreshHighlights();
        }
    }

    private void refreshHighlights() {
        if (placementMode != PlacementMode.NONE) {
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
        if (placementMode == PlacementMode.FARM) {
            disableFarmPlacementMode();
        } else if (placementMode == PlacementMode.UNIT) {
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

        if (placementMode == PlacementMode.FARM) {
            disableFarmPlacementMode();
        }

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
        if (placementMode == PlacementMode.FARM) {
            disableFarmPlacementMode();
        }

        placementMode = PlacementMode.UNIT;
        placementLevel = level;
        placementPrice = price;

        // Подсвечиваем доступные для размещения гексы
        highlightAvailableHexesForPlacement();

        showAlert("Размещение юнита", "Выберите гекс на своей территории для размещения юнита уровня " + level);
    }

    private void disableUnitPlacementMode() {
        placementMode = PlacementMode.NONE;
        placementLevel = null;
        placementPrice = null;
        clearPlacementHighlights();
    }

    private void highlightAvailableHexesForPlacement() {
        clearPlacementHighlights();

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        // Получаем все гексы базы игрока
        List<Hex> baseHexes = getPlayerBaseHexes(currentPlayer.getId());

        // Получаем все соседние гексы с базой
        Set<Hex> availableHexes = new HashSet<>();

        // 1. Добавляем гексы самой базы (своя территория)
        for (Hex baseHex : baseHexes) {
            availableHexes.add(baseHex);

            // 2. Добавляем всех соседей гексов базы (граничные гексы)
            List<Hex> neighbors = gameMapService.getNeighbors(baseHex.getX(), baseHex.getY());
            for (Hex neighbor : neighbors) {
                // Проверяем, что соседний гекс существует
                if (neighbor != null) {
                    Farm farmOnHex = farmManager.getFarmAt(neighbor.getX(), neighbor.getY());
                    if (farmOnHex != null) {
                        continue;
                    }

                    Unit existingUnit = unitManager.getUnitAt(neighbor.getX(), neighbor.getY());

                    // Создаем временный юнит для проверки canDefeat
                    Unit tempUnitForCheck = new Unit(-1, currentPlayer.getId(), neighbor.getX(), neighbor.getY(), placementLevel);

                    // Если на гексе нет юнита ИЛИ есть вражеский юнит, которого можно победить
                    if (existingUnit == null ||
                            (existingUnit.getOwnerId() != currentPlayer.getId() &&
                                    tempUnitForCheck.canDefeat(existingUnit))) {
                        availableHexes.add(neighbor);
                    }
                }
            }
        }

        // Подсвечиваем доступные гексы
        for (Hex hex : availableHexes) {
            Hexagon hexagon = getHexagonAt(hex.getX(), hex.getY());
            if (hexagon != null) {
                boolean isOwnTerritory = hex.getOwnerId() == currentPlayer.getId();
                Unit existingUnit = unitManager.getUnitAt(hex.getX(), hex.getY());
                boolean hasEnemyUnit = existingUnit != null && existingUnit.getOwnerId() != currentPlayer.getId();

                if (isOwnTerritory) {
                    // Своя территория - зеленая подсветка
                    hexagon.setHighlighted(true);
                    hexagon.setStroke(Color.LIMEGREEN);
                    hexagon.setStrokeWidth(2.0);
                } else if (hasEnemyUnit) {
                    // Вражеская территория с юнитом - оранжевая подсветка
                    hexagon.setHighlighted(true);
                    hexagon.setStroke(Color.ORANGE);
                    hexagon.setStrokeWidth(3.0);
                } else {
                    // Нейтральная/вражеская территория без юнита - фиолетовая подсветка
                    hexagon.setHighlighted(true);
                    hexagon.setStroke(Color.PURPLE);
                    hexagon.setStrokeWidth(2.0);
                }
            }
        }
    }

    private List<Hex> getPlayerBaseHexes(int playerId) {
        List<Hex> baseHexes = new ArrayList<>();

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hex hex = gameMap.getHex(x, y);
                if (hex != null && hex.getOwnerId() == playerId) {
                    baseHexes.add(hex);
                }
            }
        }

        return baseHexes;
    }

    private void clearPlacementHighlights() {
        hexagons.values().forEach(hexagon -> {
            if (hexagon.isHighlighted()) {
                hexagon.setHighlighted(false);
                hexagon.setStroke(Color.BLACK);
                hexagon.setStrokeWidth(1.0);
            }
        });
    }

    private void handleBuyFarm() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        if (placementMode == PlacementMode.UNIT) {
            disableUnitPlacementMode();
        }

        int price = farmShop.getFarmPrice(currentPlayer.getId());

        if (!farmShop.canAffordFarm(currentPlayer.getMoney(), currentPlayer.getId())) {
            return;
        }
        enableFarmPlacementMode(price);
    }

    private void enableFarmPlacementMode(int price) {
        placementMode = PlacementMode.FARM;
        placementPrice = price;

        highlightAvailableHexesForFarmPlacement();

    }

    private void highlightAvailableHexesForFarmPlacement() {
        clearPlacementHighlights();

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hex hex = gameMap.getHex(x, y);
                if (hex != null && hex.getOwnerId() == currentPlayer.getId()) {
                    Unit unit = unitManager.getUnitAt(x, y);
                    Farm farm = farmManager.getFarmAt(x, y);

                    if (unit == null && farm == null) {
                        Hexagon hexagon = getHexagonAt(x, y);
                        if (hexagon != null) {
                            hexagon.setHighlighted(true);
                            hexagon.setStroke(Color.DARKGREEN);
                            hexagon.setStrokeWidth(3.0);
                        }
                    }
                }
            }
        }
    }

    private void handleFarmPlacement(Hexagon clickedHex) {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        int x = clickedHex.getGridX();
        int y = clickedHex.getGridY();

        Hex hex = gameMap.getHex(x, y);

        if (hex.getOwnerId() != currentPlayer.getId()) {
            return;
        }

        Unit existingUnit = unitManager.getUnitAt(x, y);
        if (existingUnit != null) {
            return;
        }

        Farm existingFarm = farmManager.getFarmAt(x, y);
        if (existingFarm != null) {
            return;
        }
        Farm newFarm = farmShop.purchaseFarm(currentPlayer.getId(), x, y);
        currentPlayer.setMoney(currentPlayer.getMoney() - placementPrice);

        disableFarmPlacementMode();
        updateTurnInfo();
        initializeMap();
    }

    private void disableFarmPlacementMode() {
        placementMode = PlacementMode.NONE;
        placementPrice = null;
        clearPlacementHighlights();
    }

    private void drawFarms() {
        for (Player player : game.getPlayers()) {
            List<Farm> farms = farmManager.getPlayerFarms(player.getId());
            for (Farm farm : farms) {
                Hexagon hexagon = getHexagonAt(farm.getHexX(), farm.getHexY());
                if (hexagon != null) {
                    Text farmText = new Text("Ф");
                    farmText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
                    farmText.setFill(getPlayerTextColor(player.getId()));

                    double[] center = Hexagon.getCenterCoords(hexagon.getGridX(), hexagon.getGridY());
                    farmText.setX(center[0] - 5);
                    farmText.setY(center[1] + 5);
                    mapPane.getChildren().add(farmText);
                }
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}