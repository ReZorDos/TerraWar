package ru.kpfu.itis.view;

import javafx.geometry.Pos;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameMapPane - PURE VIEW (только отображение)
 * Вся логика делегируется в GameActionService
 */
public class GameMapPane extends VBox {

    private final GameMap gameMap;
    private final Game game;
    private final GameState gameState;

    private final GameMapService gameMapService;
    private final GameActionService gameActionService;
    private final GameTurnManager turnManager;
    private final UnitManager unitManager;

    private final Map<String, Hexagon> hexagons;
    private final Pane mapPane;
    private final Text turnInfoText;
    private final Text unitCountText;
    private final Button endTurnButton;

    private Unit selectedUnit = null;
    private List<Hex> actionHexes = null;

    public GameMapPane(GameMap gameMap, GameMapService gameMapService,
                       GameActionService gameActionService, Game game,
                       GameTurnManager turnManager, UnitManager unitManager) {
        this.gameMap = gameMap;
        this.gameMapService = gameMapService;
        this.gameActionService = gameActionService;
        this.game = game;
        this.turnManager = turnManager;
        this.unitManager = unitManager;
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

    /**
     * Инициализация UI компонентов (только отображение)
     */
    private void initializeUI() {
        HBox controlPanel = new HBox(20);
        controlPanel.setAlignment(Pos.TOP_LEFT);
        controlPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");
        controlPanel.getChildren().addAll(turnInfoText, unitCountText, endTurnButton);

        mapPane.setPrefSize(900, 550);
        turnInfoText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        unitCountText.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        this.getChildren().addAll(controlPanel, mapPane);
    }

    /**
     * Инициализация карты (только отображение)
     */
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

    /**
     * Рисует юнитов на карте (только отображение)
     */
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

    /**
     * Рисует номер/уровень юнита на гексе (только отображение)
     */
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

    /**
     * Обновляет информацию о ходе (только отображение)
     */
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

    /**
     * Возвращает цвет текста для игрока (только отображение)
     */
    private Color getPlayerTextColor(int playerId) {
        return switch (playerId) {
            case 0 -> Color.DARKRED;
            case 1 -> Color.DARKBLUE;
            default -> Color.BLACK;
        };
    }

    /**
     * Обновляет внешний вид гексагона (только отображение)
     */
    private void updateHexagonAppearance(Hexagon hexagon, Hex hexData) {
        hexagon.setColor(getOwnerColor(hexData.getOwnerId()));
    }

    /**
     * Возвращает цвет гексагона в зависимости от владельца (только отображение)
     */
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

    /**
     * Настройка обработчиков событий
     */
    private void setupEventHandlers() {
        mapPane.setOnMouseClicked(event -> {
            Hexagon clickedHex = getHexAtPixel(event.getX(), event.getY());
            if (clickedHex != null) {
                handleHexClick(clickedHex);
            }
        });

        endTurnButton.setOnAction(event -> endTurn());
    }

    /**
     * Обработчик клика по гексагону (логика здесь!)
     */
    private void handleHexClick(Hexagon clickedHex) {
        Unit unitOnHex = unitManager.getUnitAt(clickedHex.getGridX(), clickedHex.getGridY());

        // Клик по юниту текущего игрока, который может действовать
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

        // Клик по доступному гексу - выполнить действие юнита
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

        // Клик на пустую клетку - отмена выделения
        deselectUnit();
    }

    /**
     * Выбрать юнита (только UI)
     */
    private void selectUnit(Unit unit) {
        selectedUnit = unit;

        Hexagon hexagon = getHexagonAt(unit.getHexX(), unit.getHexY());
        if (hexagon != null) {
            hexagon.setSelected(true);
        }

        // ЛОГИКА ВЫЧИСЛЕНИЯ РАДИУСА В СЕРВИСЕ!
        actionHexes = gameActionService.calculateActionRadius(unit);
        refreshHighlights();

        System.out.println("[UNIT] Выбран " + unit.getUnitTypeName() +
                " (ур. " + unit.getLevel() + ", радиус: " + unit.getActionRadius() + ")");
    }

    /**
     * Отменить выделение юнита (только UI)
     */
    private void deselectUnit() {
        if (selectedUnit != null) {
            Hexagon hexagon = getHexagonAt(selectedUnit.getHexX(), selectedUnit.getHexY());
            if (hexagon != null) {
                hexagon.setSelected(false);
            }
        }

        selectedUnit = null;
        actionHexes = null;
        refreshHighlights();
    }

    /**
     * Обновить подсветку доступных гексов (только UI)
     */
    private void refreshHighlights() {
        // Сбрасываем подсветку всех гексов
        hexagons.values().forEach(hexagon -> {
            hexagon.setHighlighted(false);
            hexagon.setSelected(false);
        });

        // Если юнит выбран - подсвечиваем доступные гексы
        if (selectedUnit != null && actionHexes != null) {
            for (Hex hex : actionHexes) {
                Hexagon hexagon = getHexagonAt(hex.getX(), hex.getY());
                if (hexagon != null) {
                    hexagon.setHighlighted(true);
                }
            }

            // Выделяем гекс с выбранным юнитом
            Hexagon unitHex = getHexagonAt(selectedUnit.getHexX(), selectedUnit.getHexY());
            if (unitHex != null) {
                unitHex.setSelected(true);
            }
        }
    }

    /**
     * Завершить ход (только UI делегирование)
     */
    private void endTurn() {
        deselectUnit();
        turnManager.endPlayerTurn();
        turnManager.startPlayerTurn();

        Player nextPlayer = game.getCurrentPlayer();
        System.out.println("\n=== НОВЫЙ ХОД ===");
        System.out.println("[TURN] Ход перешёл к " + nextPlayer.getName());
        System.out.println("[MONEY] " + nextPlayer.getName() + " получил +" +
                nextPlayer.getIncome() + " монет (всего: " + nextPlayer.getMoney() + ")");
        System.out.println();

        updateCurrentPlayer();
        gameState.clearSelection();
        refreshHighlights();
        updateTurnInfo();
        initializeMap();
    }

    /**
     * Обновить текущего игрока (только UI)
     */
    private void updateCurrentPlayer() {
        if (game.getCurrentPlayer() != null) {
            gameState.setCurrentPlayerId(game.getCurrentPlayer().getId());
        }
    }

    /**
     * Получить гексагон в точке пикселей (только UI)
     */
    public Hexagon getHexAtPixel(double mouseX, double mouseY) {
        return hexagons.values().stream()
                .filter(hexagon -> hexagon.getBoundsInParent().contains(mouseX, mouseY))
                .findFirst()
                .orElse(null);
    }

    /**
     * Получить гексагон по координатам сетки (только UI)
     */
    public Hexagon getHexagonAt(int gridX, int gridY) {
        String key = gridX + "," + gridY;
        return hexagons.get(key);
    }
}