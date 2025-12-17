package ru.kpfu.itis.view;

import javafx.scene.paint.Color;
import ru.kpfu.itis.enums.PlacementMode;
import ru.kpfu.itis.model.Farm;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Hex;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.model.Tower;
import ru.kpfu.itis.model.Unit;
import ru.kpfu.itis.network.service.OnlineGameManager;
import ru.kpfu.itis.service.*;

import java.util.*;
import java.util.function.BiConsumer;

public class PlacementController {

    private final GameMap gameMap;
    private final Game game;
    private final GameActionService gameActionService;
    private final GameMapService gameMapService;
    private final UnitManager unitManager;
    private final UnitShop unitShop;
    private final FarmManager farmManager;
    private final FarmShop farmShop;
    private final TowerManager towerManager;
    private final TowerShop towerShop;
    private final MapRenderer mapRenderer;
    private final ImageCache imageCache;
    private final OnlineGameManager onlineGameManager;
    private Runnable onMapRefresh;
    private Runnable onTurnInfoRefresh;
    private Runnable onHighlightsRefresh;
    private java.util.function.BiConsumer<String, String> onShowAlert;
    private Runnable onStateUpdate;
    private java.util.function.Supplier<Boolean> onCheckMyTurn;
    private PlacementMode placementMode = PlacementMode.NONE;
    private Integer placementLevel = null;
    private Integer placementPrice = null;

    public PlacementController(GameMap gameMap,
                               Game game,
                               GameActionService gameActionService,
                               GameMapService gameMapService,
                               UnitManager unitManager,
                               UnitShop unitShop,
                               FarmManager farmManager,
                               FarmShop farmShop,
                               TowerManager towerManager,
                               TowerShop towerShop,
                               MapRenderer mapRenderer,
                               ImageCache imageCache,
                               OnlineGameManager onlineGameManager) {
        this.gameMap = gameMap;
        this.game = game;
        this.gameActionService = gameActionService;
        this.gameMapService = gameMapService;
        this.unitManager = unitManager;
        this.unitShop = unitShop;
        this.farmManager = farmManager;
        this.farmShop = farmShop;
        this.towerManager = towerManager;
        this.towerShop = towerShop;
        this.mapRenderer = mapRenderer;
        this.imageCache = imageCache;
        this.onlineGameManager = onlineGameManager;
    }

    public void setCallbacks(Runnable onMapRefresh,
                            Runnable onTurnInfoRefresh,
                            Runnable onHighlightsRefresh,
                            java.util.function.BiConsumer<String, String> onShowAlert,
                            Runnable onStateUpdate,
                            java.util.function.Supplier<Boolean> onCheckMyTurn) {
        this.onMapRefresh = onMapRefresh;
        this.onTurnInfoRefresh = onTurnInfoRefresh;
        this.onHighlightsRefresh = onHighlightsRefresh;
        this.onShowAlert = onShowAlert;
        this.onStateUpdate = onStateUpdate;
        this.onCheckMyTurn = onCheckMyTurn;
    }
    
    private boolean isMyTurn() {
        if (onCheckMyTurn != null) {
            return onCheckMyTurn.get();
        }
        return true;
    }

    public boolean isPlacementActive() {
        return placementMode != PlacementMode.NONE;
    }

    public void cancelPlacementIfActive() {
        switch (placementMode) {
            case FARM -> disableFarmPlacementMode();
            case UNIT -> disableUnitPlacementMode();
            case TOWER -> disableTowerPlacementMode();
            default -> { }
        }
    }

    public boolean handleHexClick(TexturedHexagon clickedHex) {
        if (!isMyTurn()) {
            return false;
        }
        
        return switch (placementMode) {
            case FARM -> {
                handleFarmPlacement(clickedHex);
                yield true;
            }
            case UNIT -> {
                handleUnitPlacement(clickedHex);
                yield true;
            }
            case TOWER -> {
                handleTowerPlacement(clickedHex);
                yield true;
            }
            default -> false;
        };
    }

    public void handleBuyUnit() {
        if (!isMyTurn()) {
            if (onShowAlert != null) {
                onShowAlert.accept("Не ваш ход", "Дождитесь своего хода");
            }
            return;
        }
        
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        if (placementMode == PlacementMode.FARM) {
            disableFarmPlacementMode();
        } else if (placementMode == PlacementMode.TOWER) {
            disableTowerPlacementMode();
        }

        UnitShopUI unitShopUI = new UnitShopUI(unitShop, imageCache, () -> {});

        Integer selectedLevel = unitShopUI.showAndWait(currentPlayer.getMoney());
        if (selectedLevel != null) {
            int price = unitShop.getUnitPrice(selectedLevel);
            if (!unitShop.canAffordUnit(currentPlayer.getMoney(), selectedLevel)) {
                if (onShowAlert != null) {
                    onShowAlert.accept("Недостаточно денег",
                            "Юнит уровня " + selectedLevel + " стоит " + price + " монет\n");
                }
                return;
            }
            enableUnitPlacementMode(selectedLevel, price);
        }
    }

    public void handleBuyFarm() {
        if (!isMyTurn()) {
            if (onShowAlert != null) {
                onShowAlert.accept("Не ваш ход", "Дождитесь своего хода");
            }
            return;
        }
        
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        if (placementMode == PlacementMode.UNIT) {
            disableUnitPlacementMode();
        } else if (placementMode == PlacementMode.TOWER) {
            disableTowerPlacementMode();
        }

        FarmShopUI farmShopUI = new FarmShopUI(farmShop, imageCache, () -> {});

        boolean selected = farmShopUI.showAndWait(currentPlayer.getMoney(), currentPlayer.getId());
        if (selected) {
            int price = farmShop.getFarmPrice(currentPlayer.getId());
            if (!farmShop.canAffordFarm(currentPlayer.getMoney(), currentPlayer.getId())) {
                if (onShowAlert != null) {
                    onShowAlert.accept("Недостаточно денег",
                            "Ферма стоит " + price + " монет");
                }
                return;
            }
            enableFarmPlacementMode(price);
        }
    }

    public void handleBuyTower() {
        if (!isMyTurn()) {
            if (onShowAlert != null) {
                onShowAlert.accept("Не ваш ход", "Дождитесь своего хода");
            }
            return;
        }
        
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        if (placementMode == PlacementMode.UNIT) {
            disableUnitPlacementMode();
        } else if (placementMode == PlacementMode.FARM) {
            disableFarmPlacementMode();
        }

        TowerShopUI towerShopUI = new TowerShopUI(towerShop, imageCache, () -> {});

        Integer selectedLevel = towerShopUI.showAndWait(currentPlayer.getMoney(), currentPlayer.getId());
        if (selectedLevel != null) {
            int price = towerShop.getTowerPrice(currentPlayer.getId(), selectedLevel);
            if (!towerShop.canAffordTower(currentPlayer.getMoney(), selectedLevel, currentPlayer.getId())) {
                if (onShowAlert != null) {
                    onShowAlert.accept("Недостаточно денег",
                            "Башня уровня " + selectedLevel + " стоит " + price + " монет");
                }
                return;
            }
            enableTowerPlacementMode(selectedLevel, price);
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
        highlightAvailableHexesForPlacement(this::highlightUnitPlacementHex);
        if (onShowAlert != null) {
            onShowAlert.accept("Размещение юнита", "Выберите гекс на своей территории для размещения юнита уровня " + level);
        }
    }

    private void disableUnitPlacementMode() {
        placementMode = PlacementMode.NONE;
        placementLevel = null;
        placementPrice = null;
        clearPlacementHighlights();
        if (onHighlightsRefresh != null) {
            onHighlightsRefresh.run();
        }
    }

    private void handleUnitPlacement(TexturedHexagon clickedHex) {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        int x = clickedHex.getGridX();
        int y = clickedHex.getGridY();
        Hex hex = gameMap.getHex(x, y);

        if (!canPlaceUnitOnHex(currentPlayer.getId(), x, y)) {
            if (onShowAlert != null) {
                onShowAlert.accept("Ошибка",
                        "Можно размещать юнитов только на своей территории ИЛИ на соседних с вашей базой гексах!");
            }
            return;
        }

        if (farmManager.getFarmAt(x, y) != null) return;
        if (towerManager.getTowerAt(x, y) != null) return;

        Unit existingUnit = unitManager.getUnitAt(x, y);
        if (existingUnit != null) {
            if (existingUnit.getOwnerId() != currentPlayer.getId()) {
                Unit tempUnitForCheck = new Unit(-1, currentPlayer.getId(), x, y, placementLevel);
                if (tempUnitForCheck.canDefeat(existingUnit)) {
                    unitManager.removeUnit(existingUnit.getId());
                } else {
                    if (onShowAlert != null) {
                        onShowAlert.accept("Ошибка",
                                "Нельзя разместить юнита уровня " + placementLevel +
                                        " на вражеского юнита уровня " + existingUnit.getLevel() + "!");
                    }
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
        if (onTurnInfoRefresh != null) onTurnInfoRefresh.run();
        if (onMapRefresh != null) onMapRefresh.run();
        if (onStateUpdate != null) onStateUpdate.run();
    }

    private boolean canPlaceUnitOnHex(int playerId, int hexX, int hexY) {
        Hex targetHex = gameMap.getHex(hexX, hexY);
        if (targetHex == null) return false;
        if (farmManager.getFarmAt(hexX, hexY) != null) return false;
        if (towerManager.getTowerAt(hexX, hexY) != null) return false;

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

    private void enableFarmPlacementMode(int price) {
        placementMode = PlacementMode.FARM;
        placementPrice = price;
        highlightAvailableHexesForPlacement(this::highlightFarmPlacementHex);
    }

    private void handleFarmPlacement(TexturedHexagon clickedHex) {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        int x = clickedHex.getGridX();
        int y = clickedHex.getGridY();
        Hex hex = gameMap.getHex(x, y);

        if (hex.getOwnerId() != currentPlayer.getId()) {
            return;
        }

        if (unitManager.getUnitAt(x, y) != null) return;
        if (farmManager.getFarmAt(x, y) != null) return;
        if (towerManager.getTowerAt(x, y) != null) return;

        farmShop.purchaseFarm(currentPlayer.getId(), x, y);
        currentPlayer.setMoney(currentPlayer.getMoney() - placementPrice);
        disableFarmPlacementMode();
        if (onTurnInfoRefresh != null) onTurnInfoRefresh.run();
        if (onMapRefresh != null) onMapRefresh.run();
        if (onStateUpdate != null) onStateUpdate.run();
    }

    private void disableFarmPlacementMode() {
        placementMode = PlacementMode.NONE;
        placementPrice = null;
        clearPlacementHighlights();
        if (onHighlightsRefresh != null) {
            onHighlightsRefresh.run();
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
        highlightAvailableHexesForPlacement(this::highlightTowerPlacementHex);
        if (onShowAlert != null) {
            onShowAlert.accept("Размещение башни",
                    "Выберите гекс на своей территории для размещения башни уровня " + level);
        }
    }

    private void handleTowerPlacement(TexturedHexagon clickedHex) {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        int x = clickedHex.getGridX();
        int y = clickedHex.getGridY();
        Hex hex = gameMap.getHex(x, y);

        if (hex.getOwnerId() != currentPlayer.getId()) {
            if (onShowAlert != null) {
                onShowAlert.accept("Ошибка", "Башню можно ставить только на свою территорию!");
            }
            return;
        }

        if (unitManager.getUnitAt(x, y) != null) {
            if (onShowAlert != null) {
                onShowAlert.accept("Ошибка", "На этом гексе уже стоит юнит!");
            }
            return;
        }

        if (farmManager.getFarmAt(x, y) != null) {
            if (onShowAlert != null) {
                onShowAlert.accept("Ошибка", "На этом гексе уже стоит ферма!");
            }
            return;
        }

        if (towerManager.getTowerAt(x, y) != null) {
            if (onShowAlert != null) {
                onShowAlert.accept("Ошибка", "На этом гексе уже стоит башня!");
            }
            return;
        }

        towerShop.purchaseTower(currentPlayer.getId(), x, y, placementLevel);
        currentPlayer.setMoney(currentPlayer.getMoney() - placementPrice);
        disableTowerPlacementMode();
        if (onTurnInfoRefresh != null) onTurnInfoRefresh.run();
        if (onMapRefresh != null) onMapRefresh.run();
        if (onStateUpdate != null) onStateUpdate.run();
    }

    private void disableTowerPlacementMode() {
        placementMode = PlacementMode.NONE;
        placementLevel = null;
        placementPrice = null;
        clearPlacementHighlights();
        if (onHighlightsRefresh != null) {
            onHighlightsRefresh.run();
        }
    }

    private void highlightAvailableHexesForPlacement(BiConsumer<TexturedHexagon, Player> highlightStrategy) {
        clearPlacementHighlights();
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hex hex = gameMap.getHex(x, y);
                if (hex != null && hex.getOwnerId() == currentPlayer.getId()) {
                    TexturedHexagon hexagon = mapRenderer.getHexagonAt(x, y);
                    if (hexagon != null) {
                        highlightStrategy.accept(hexagon, currentPlayer);
                    }
                }
            }
        }
    }

    private void highlightUnitPlacementHex(TexturedHexagon hexagon, Player currentPlayer) {
        Hex hex = gameMap.getHex(hexagon.getGridX(), hexagon.getGridY());
        if (hex == null) return;

        Unit unit = unitManager.getUnitAt(hexagon.getGridX(), hexagon.getGridY());
        Farm farm = farmManager.getFarmAt(hexagon.getGridX(), hexagon.getGridY());
        Tower tower = towerManager.getTowerAt(hexagon.getGridX(), hexagon.getGridY());

        if (farm != null || tower != null) {
            return;
        }

        Set<Hex> availableHexes = new HashSet<>();
        List<Hex> baseHexes = getPlayerBaseHexes(currentPlayer.getId());
        availableHexes.addAll(baseHexes);

        for (Hex baseHex : baseHexes) {
            List<Hex> neighbors = gameMapService.getNeighbors(baseHex.getX(), baseHex.getY());
            availableHexes.addAll(neighbors);
        }

        boolean isAvailable = availableHexes.stream()
                .anyMatch(h -> h.getX() == hexagon.getGridX() && h.getY() == hexagon.getGridY());

        if (!isAvailable) return;

        boolean hasEnemyUnit = unit != null && unit.getOwnerId() != currentPlayer.getId();

        if (hex.getOwnerId() == currentPlayer.getId()) {
            hexagon.setHighlighted(true);
            hexagon.setStroke(Color.LIMEGREEN);
            hexagon.setStrokeWidth(2.0);
        } else if (hasEnemyUnit) {
            Unit tempUnitForCheck = new Unit(-1, currentPlayer.getId(), hexagon.getGridX(), hexagon.getGridY(), placementLevel);
            if (tempUnitForCheck.canDefeat(unit)) {
                hexagon.setHighlighted(true);
                hexagon.setStroke(Color.ORANGE);
                hexagon.setStrokeWidth(3.0);
            }
        } else {
            hexagon.setHighlighted(true);
            hexagon.setStroke(Color.PURPLE);
            hexagon.setStrokeWidth(2.0);
        }
    }

    private void highlightFarmPlacementHex(TexturedHexagon hexagon, Player currentPlayer) {
        if (unitManager.getUnitAt(hexagon.getGridX(), hexagon.getGridY()) != null) return;
        if (farmManager.getFarmAt(hexagon.getGridX(), hexagon.getGridY()) != null) return;
        if (towerManager.getTowerAt(hexagon.getGridX(), hexagon.getGridY()) != null) return;

        hexagon.setHighlighted(true);
        hexagon.setStroke(Color.DARKGREEN);
        hexagon.setStrokeWidth(3.0);
    }

    private void highlightTowerPlacementHex(TexturedHexagon hexagon, Player currentPlayer) {
        if (unitManager.getUnitAt(hexagon.getGridX(), hexagon.getGridY()) != null) return;
        if (farmManager.getFarmAt(hexagon.getGridX(), hexagon.getGridY()) != null) return;
        if (towerManager.getTowerAt(hexagon.getGridX(), hexagon.getGridY()) != null) return;

        hexagon.setHighlighted(true);
        hexagon.setStroke(Color.PURPLE);
        hexagon.setStrokeWidth(3.0);
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
        mapRenderer.getHexagons().values().forEach(hexagon -> {
            if (hexagon.isHighlighted()) {
                hexagon.setHighlighted(false);
                hexagon.setStroke(Color.BLACK);
                hexagon.setStrokeWidth(1.0);
            }
        });
    }
}
