package ru.kpfu.itis.view;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
    private final TowerManager towerManager;
    private final TowerShop towerShop;
    private final Button buyTowerButton;

    private final Map<String, Image> imageCache = new HashMap<>();

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
                       FarmShop farmShop,
                       TowerManager towerManager,
                       TowerShop towerShop) {
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
        this.towerManager = towerManager;
        this.towerShop = towerShop;

        this.buyFarmButton = new Button("Купить ферму");
        this.buyUnitButton = new Button("Купить юнит");
        this.buyTowerButton = new Button("Купить башню");
        this.gameState = new GameState();
        this.hexagons = new HashMap<>();
        this.mapPane = new Pane();

        gameState.setCurrentPlayerId(game.getCurrentPlayer().getId());

        turnInfoText = new Text();
        unitCountText = new Text();
        endTurnButton = new Button("Завершить ход");

        initializeUI();
        loadImages();
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
        drawTowers();
    }

    private void loadImages() {
        try {
            imageCache.put("unit_1", new Image(getClass().getResourceAsStream("/unit_1.png")));
            imageCache.put("unit_2", new Image(getClass().getResourceAsStream("/unit_2.png")));
            imageCache.put("unit_3", new Image(getClass().getResourceAsStream("/unit_3.png")));

            imageCache.put("farm", new Image(getClass().getResourceAsStream("/farm.png")));

            imageCache.put("tower_1", new Image(getClass().getResourceAsStream("/tower_1.png")));
            imageCache.put("tower_2", new Image(getClass().getResourceAsStream("/tower_2.png")));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображений: " + e.getMessage());
        }
    }

    private void drawUnits() {
        mapPane.getChildren().removeIf(node ->
                (node instanceof StackPane &&
                        ((StackPane) node).getUserData() != null &&
                        ((String) ((StackPane) node).getUserData()).startsWith("UNIT_STACK_"))
        );

        List<Unit> allUnits = unitManager.getAllUnits();
        for (Unit unit : allUnits) {
            drawUnitImageWithNumber(unit);
        }
    }

    private void drawUnitImageWithNumber(Unit unit) {
        Hexagon hexagon = getHexagonAt(unit.getHexX(), unit.getHexY());
        if (hexagon == null) return;

        StackPane stackPane = new StackPane();
        stackPane.setUserData("UNIT_STACK_" + unit.getHexX() + "_" + unit.getHexY());

        Text levelText = new Text(String.valueOf(unit.getLevel()));
        levelText.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        levelText.setFill(Color.WHITE);
        levelText.setStroke(Color.BLACK);
        levelText.setStrokeWidth(2.0);
        stackPane.getChildren().add(levelText);

        Image image = imageCache.get("unit_" + unit.getLevel());
        if (image != null) {
            ImageView unitImage = new ImageView(image);
            double imageSize = Hexagon.SIZE * 1.5;
            unitImage.setFitWidth(imageSize);
            unitImage.setFitHeight(imageSize);
            unitImage.setPreserveRatio(true);
            unitImage.setUserData("UNIT");

            stackPane.getChildren().add(unitImage);
        }

        double[] center = hexagon.getActualCenter();
        double centerX = center[0];
        double centerY = center[1];

        double stackWidth = Hexagon.SIZE * Math.sqrt(3) * 2;
        double stackHeight = Hexagon.SIZE * 2;
        stackPane.setPrefSize(stackWidth, stackHeight);

        stackPane.setLayoutX(centerX - stackWidth / 2);
        stackPane.setLayoutY(centerY - stackHeight / 2);

        mapPane.getChildren().add(stackPane);
    }

    private void drawFarms() {
        mapPane.getChildren().removeIf(node ->
                (node instanceof StackPane &&
                        ((StackPane) node).getUserData() != null &&
                        ((String) ((StackPane) node).getUserData()).startsWith("FARM_STACK_"))
        );

        for (Player player : game.getPlayers()) {
            List<Farm> farms = farmManager.getPlayerFarms(player.getId());
            for (Farm farm : farms) {
                drawFarmImageWithSymbol(farm, player.getId());
            }
        }
    }

    private void drawFarmImageWithSymbol(Farm farm, int playerId) {
        Hexagon hexagon = getHexagonAt(farm.getHexX(), farm.getHexY());
        if (hexagon == null) return;

        StackPane stackPane = new StackPane();
        stackPane.setUserData("FARM_STACK_" + farm.getHexX() + "_" + farm.getHexY());

        Text farmSymbol = new Text("F");
        farmSymbol.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        farmSymbol.setFill(Color.WHITE);
        farmSymbol.setStroke(Color.WHITE);
        farmSymbol.setStrokeWidth(2.0);

        stackPane.getChildren().add(farmSymbol);

        Image image = imageCache.get("farm");
        if (image != null) {
            ImageView farmImage = new ImageView(image);
            double imageSize = Hexagon.SIZE * 1.5;
            farmImage.setFitWidth(imageSize);
            farmImage.setFitHeight(imageSize);
            farmImage.setPreserveRatio(true);
            farmImage.setUserData("FARM");

            stackPane.getChildren().add(farmImage);
        }

        double[] center = hexagon.getActualCenter();
        double centerX = center[0];
        double centerY = center[1];

        double stackWidth = Hexagon.SIZE * Math.sqrt(3) * 2;
        double stackHeight = Hexagon.SIZE * 2;
        stackPane.setPrefSize(stackWidth, stackHeight);

        stackPane.setLayoutX(centerX - stackWidth / 2);
        stackPane.setLayoutY(centerY - stackHeight / 2);

        mapPane.getChildren().add(stackPane);
    }

    private void drawTowers() {
        mapPane.getChildren().removeIf(node ->
                (node instanceof StackPane &&
                        ((StackPane) node).getUserData() != null &&
                        ((String) ((StackPane) node).getUserData()).startsWith("TOWER_STACK_"))
        );

        for (Player player : game.getPlayers()) {
            List<Tower> towers = towerManager.getPlayerTowers(player.getId());
            for (Tower tower : towers) {
                drawTowerImageWithSymbol(tower, player.getId());
            }
        }
    }

    private void drawTowerImageWithSymbol(Tower tower, int playerId) {
        Hexagon hexagon = getHexagonAt(tower.getHexX(), tower.getHexY());
        if (hexagon == null) return;

        StackPane stackPane = new StackPane();
        stackPane.setUserData("TOWER_STACK_" + tower.getHexX() + "_" + tower.getHexY());

        Text towerSymbol = new Text("Б" + tower.getLevel());
        towerSymbol.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        towerSymbol.setFill(Color.WHITE);
        towerSymbol.setStroke(Color.WHITE);
        towerSymbol.setStrokeWidth(2.0);

        stackPane.getChildren().add(towerSymbol);

        Image image = imageCache.get("tower_" + tower.getLevel());
        if (image != null) {
            ImageView towerImage = new ImageView(image);
            double imageSize = Hexagon.SIZE * 1.5;
            towerImage.setFitWidth(imageSize);
            towerImage.setFitHeight(imageSize);
            towerImage.setPreserveRatio(true);
            towerImage.setUserData("TOWER");

            stackPane.getChildren().add(towerImage);
        }

        double[] center = hexagon.getActualCenter();
        double centerX = center[0];
        double centerY = center[1];

        double stackWidth = Hexagon.SIZE * Math.sqrt(3) * 2;
        double stackHeight = Hexagon.SIZE * 2;
        stackPane.setPrefSize(stackWidth, stackHeight);

        stackPane.setLayoutX(centerX - stackWidth / 2);
        stackPane.setLayoutY(centerY - stackHeight / 2);

        mapPane.getChildren().add(stackPane);
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
        buyTowerButton.setOnAction(event -> handleBuyTower());
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

        if (placementMode == PlacementMode.TOWER) {
            handleTowerPlacement(clickedHex);
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

        Tower towerOnHex = towerManager.getTowerAt(x, y);
        if (towerOnHex != null) {
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

        Tower towerOnHex = towerManager.getTowerAt(hexX, hexY);
        if (towerOnHex != null) {
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
        } else if (placementMode == PlacementMode.TOWER) {
            disableTowerPlacementMode();
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
        } else if (placementMode == PlacementMode.TOWER) {
            disableTowerPlacementMode();
        }

        List<Integer> levels = Arrays.asList(1, 2, 3);
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(1, levels);
        dialog.setTitle("Покупка юнита");

        StringBuilder header = new StringBuilder();
        header.append("Выберите уровень юнита\n");
        header.append("Ваши деньги: ").append(currentPlayer.getMoney()).append(" монет\n\n");
        header.append("Стоимость юнитов:\n");

        for (int level : levels) {
            int price = unitShop.getUnitPrice(level);
            header.append("Уровень ").append(level).append(": ").append(price)
                    .append(" монет \n");
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

            enableUnitPlacementMode(level, price);
        }
    }

    private void enableUnitPlacementMode(int level, int price) {
        if (placementMode == PlacementMode.FARM) {
            disableFarmPlacementMode();
        } else if (placementMode == PlacementMode.TOWER) {
            disableTowerPlacementMode();
        }

        placementMode = PlacementMode.UNIT;
        placementLevel = level;
        placementPrice = price;

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

        List<Hex> baseHexes = getPlayerBaseHexes(currentPlayer.getId());
        Set<Hex> availableHexes = new HashSet<>();

        for (Hex baseHex : baseHexes) {
            availableHexes.add(baseHex);

            List<Hex> neighbors = gameMapService.getNeighbors(baseHex.getX(), baseHex.getY());
            for (Hex neighbor : neighbors) {
                if (neighbor != null) {
                    Farm farmOnHex = farmManager.getFarmAt(neighbor.getX(), neighbor.getY());
                    if (farmOnHex != null) {
                        continue;
                    }

                    Tower towerOnHex = towerManager.getTowerAt(neighbor.getX(), neighbor.getY());
                    if (towerOnHex != null) {
                        continue;
                    }

                    Unit existingUnit = unitManager.getUnitAt(neighbor.getX(), neighbor.getY());
                    Unit tempUnitForCheck = new Unit(-1, currentPlayer.getId(), neighbor.getX(), neighbor.getY(), placementLevel);

                    if (existingUnit == null ||
                            (existingUnit.getOwnerId() != currentPlayer.getId() &&
                                    tempUnitForCheck.canDefeat(existingUnit))) {
                        availableHexes.add(neighbor);
                    }
                }
            }
        }

        for (Hex hex : availableHexes) {
            Hexagon hexagon = getHexagonAt(hex.getX(), hex.getY());
            if (hexagon != null) {
                boolean isOwnTerritory = hex.getOwnerId() == currentPlayer.getId();
                Unit existingUnit = unitManager.getUnitAt(hex.getX(), hex.getY());
                boolean hasEnemyUnit = existingUnit != null && existingUnit.getOwnerId() != currentPlayer.getId();

                if (isOwnTerritory) {
                    hexagon.setHighlighted(true);
                    hexagon.setStroke(Color.LIMEGREEN);
                    hexagon.setStrokeWidth(2.0);
                } else if (hasEnemyUnit) {
                    hexagon.setHighlighted(true);
                    hexagon.setStroke(Color.ORANGE);
                    hexagon.setStrokeWidth(3.0);
                } else {
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
        } else if (placementMode == PlacementMode.TOWER) {
            disableTowerPlacementMode();
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
                    Tower tower = towerManager.getTowerAt(x, y);

                    if (unit == null && farm == null && tower == null) {
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

        Tower existingTower = towerManager.getTowerAt(x, y);
        if (existingTower != null) {
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

    private void handleBuyTower() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        if (placementMode == PlacementMode.UNIT) {
            disableUnitPlacementMode();
        } else if (placementMode == PlacementMode.FARM) {
            disableFarmPlacementMode();
        }

        List<Integer> levels = Arrays.asList(1, 2);
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(1, levels);
        dialog.setTitle("Покупка башни");

        StringBuilder header = new StringBuilder();
        header.append("Выберите уровень башни\n");
        header.append("Ваши деньги: ").append(currentPlayer.getMoney()).append(" монет\n\n");
        header.append("Стоимость башен:\n");

        for (int level : levels) {
            int price = towerShop.getTowerPrice(currentPlayer.getId(), level);
            header.append("Уровень ").append(level).append(": ").append(price);

            if (level == 1) {
                header.append(" монет (блокирует юниты уровня 1)\n");
            } else {
                header.append(" монет (блокирует юниты уровня 1-2)\n");
            }
        }

        dialog.setHeaderText(header.toString());
        dialog.setContentText("Уровень:");

        Optional<Integer> result = dialog.showAndWait();
        if (result.isPresent()) {
            int level = result.get();
            int price = towerShop.getTowerPrice(currentPlayer.getId(), level);

            if (!towerShop.canAffordTower(currentPlayer.getMoney(), level, currentPlayer.getId())) {
                showAlert("Недостаточно денег",
                        "Башня уровня " + level + " стоит " + price + " монет");
                return;
            }

            enableTowerPlacementMode(level, price);
        }
    }

    private void enableTowerPlacementMode(int level, int price) {
        if (placementMode == PlacementMode.UNIT) {
            disableUnitPlacementMode();
        } else if (placementMode == PlacementMode.FARM) {
            disableFarmPlacementMode();
        }

        placementMode = PlacementMode.TOWER;
        placementLevel = level;
        placementPrice = price;

        highlightAvailableHexesForTowerPlacement();
        showAlert("Размещение башни",
                "Выберите гекс на своей территории для размещения башни уровня " + level);
    }

    private void highlightAvailableHexesForTowerPlacement() {
        clearPlacementHighlights();
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hex hex = gameMap.getHex(x, y);
                if (hex != null && hex.getOwnerId() == currentPlayer.getId()) {
                    Unit unit = unitManager.getUnitAt(x, y);
                    Farm farm = farmManager.getFarmAt(x, y);
                    Tower tower = towerManager.getTowerAt(x, y);

                    if (unit == null && farm == null && tower == null) {
                        Hexagon hexagon = getHexagonAt(x, y);
                        if (hexagon != null) {
                            hexagon.setHighlighted(true);
                            hexagon.setStroke(Color.PURPLE);
                            hexagon.setStrokeWidth(3.0);
                        }
                    }
                }
            }
        }
    }

    private void handleTowerPlacement(Hexagon clickedHex) {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        int x = clickedHex.getGridX();
        int y = clickedHex.getGridY();
        Hex hex = gameMap.getHex(x, y);

        if (hex.getOwnerId() != currentPlayer.getId()) {
            showAlert("Ошибка", "Башню можно ставить только на свою территорию!");
            return;
        }

        Unit existingUnit = unitManager.getUnitAt(x, y);
        if (existingUnit != null) {
            showAlert("Ошибка", "На этом гексе уже стоит юнит!");
            return;
        }

        Farm existingFarm = farmManager.getFarmAt(x, y);
        if (existingFarm != null) {
            showAlert("Ошибка", "На этом гексе уже стоит ферма!");
            return;
        }

        Tower existingTower = towerManager.getTowerAt(x, y);
        if (existingTower != null) {
            showAlert("Ошибка", "На этом гексе уже стоит башня!");
            return;
        }

        Tower newTower = towerShop.purchaseTower(currentPlayer.getId(), x, y, placementLevel);
        currentPlayer.setMoney(currentPlayer.getMoney() - placementPrice);

        disableTowerPlacementMode();
        updateTurnInfo();
        initializeMap();
    }

    private void disableTowerPlacementMode() {
        placementMode = PlacementMode.NONE;
        placementLevel = null;
        placementPrice = null;
        clearPlacementHighlights();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}