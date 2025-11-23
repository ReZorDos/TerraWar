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
import ru.kpfu.itis.service.Game;
import ru.kpfu.itis.service.GameMapService;
import ru.kpfu.itis.state.GameState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameMapPane extends VBox {

    private final GameMap gameMap;
    private final GameMapService gameMapService;
    private final GameState gameState;
    private final Map<String, Hexagon> hexagons;
    private final Game game;
    private final Pane mapPane;
    private final Text turnInfoText;
    private final Button endTurnButton;

    public GameMapPane(GameMap gameMap, GameMapService gameMapService, Game game) {
        this.gameMap = gameMap;
        this.gameMapService = gameMapService;
        this.game = game;
        this.gameState = new GameState();
        this.hexagons = new HashMap<>();
        this.mapPane = new Pane();

        gameState.setCurrentPlayerId(game.getCurrentPlayer().getId());

        turnInfoText = new Text();
        endTurnButton = new Button("End the turn");

        initializeUI();
        initializeMap();
        setupEventHandlers();
        updateTurnInfo();
    }

    private void initializeUI() {
        HBox controlPanel = new HBox(10);
        controlPanel.setAlignment(Pos.TOP_RIGHT);
        controlPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

        controlPanel.getChildren().addAll(turnInfoText, endTurnButton);

        mapPane.setPrefSize(900, 550);
        turnInfoText.setFont(Font.font("Arial", FontWeight.BOLD, 14));

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
    }

    private void updateTurnInfo() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            turnInfoText.setText(currentPlayer.getName() +
                    " | Balance: " + currentPlayer.getMoney() +
                    " $ | Income: +" + currentPlayer.getIncome() + " $");
            turnInfoText.setFill(getPlayerTextColor(currentPlayer.getId()));
        }
    }

    private Color getPlayerTextColor(int playerId) {
        switch (playerId) {
            case 0: return Color.DARKRED;
            case 1: return Color.DARKBLUE;
            case 2: return Color.DARKGREEN;
            case 3: return Color.PURPLE;
            default: return Color.BLACK;
        }
    }

    private void updateHexagonAppearance(Hexagon hexagon, Hex hexData) {
        hexagon.setHighlighted(false);
        hexagon.setSelected(false);
        hexagon.setColor(getOwnerColor(hexData.getOwnerId()));
    }

    private Color getOwnerColor(int ownerId) {
        if (ownerId == -1) {
            return Color.LIGHTGRAY;
        }
        switch (ownerId) {
            case 0: return Color.RED;
            case 1: return Color.BLUE;
            case 2: return Color.GREEN;
            case 3: return Color.PURPLE;
            default: return Color.ORANGE;
        }
    }

    private void setupEventHandlers() {
        mapPane.setOnMouseClicked(event -> {
            Hexagon clickedHex = getHexAtPixel(event.getX(), event.getY());
            if (clickedHex != null) {
                handleHexClick(clickedHex);
            }
        });
        endTurnButton.setOnAction(event -> {
            endTurn();
        });
    }

    private void handleHexClick(Hexagon clickedHex) {
        Hex clickedHexData = gameMap.getHex(clickedHex.getGridX(), clickedHex.getGridY());
        if (!gameState.canInteractWithHex(clickedHexData)) {
            gameState.clearSelection();
            refreshHighlights();
            return;
        }

        if (clickedHexData.getOwnerId() == gameState.getCurrentPlayerId()) {
            selectHex(clickedHex);
        } else if (gameState.isHighlightedNeighbor(clickedHex)) {
            captureHex(clickedHex);
            gameState.clearSelection();
            refreshHighlights();
        } else {
            gameState.clearSelection();
            refreshHighlights();
        }
    }

    private void selectHex(Hexagon hexagon) {
        gameState.clearSelection();

        gameState.setSelectedHexagon(hexagon);
        gameState.setSelectedOwnerId(gameMap.getHex(hexagon.getGridX(), hexagon.getGridY()).getOwnerId());
        hexagon.setSelected(true);

        List<Hex> neighbors = gameMapService.getNeighbors(hexagon.getGridX(), hexagon.getGridY());
        gameState.setHighlightedNeighbors(neighbors);
        refreshHighlights();
    }

    private void captureHex(Hexagon neighborHex) {
        Hex neighborData = gameMap.getHex(neighborHex.getGridX(), neighborHex.getGridY());
        neighborData.setOwnerId(gameState.getCurrentPlayerId());
        updateHexagonAppearance(neighborHex, neighborData);

        updateTurnInfo();
    }

    private void endTurn() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            currentPlayer.setMoney(currentPlayer.getMoney() + currentPlayer.getIncome());
        }

        game.nextTurn();
        updateCurrentPlayer();
        gameState.clearSelection();
        refreshHighlights();
        updateTurnInfo();
    }

    private void updateCurrentPlayer() {
        if (game.getCurrentPlayer() != null) {
            gameState.setCurrentPlayerId(game.getCurrentPlayer().getId());
        }
    }

    private void refreshHighlights() {
        hexagons.values().forEach(hexagon -> hexagon.setHighlighted(false));

        if (gameState.getSelectedHexagon() != null) {
            gameState.getSelectedHexagon().setSelected(true);
        }

        if (gameState.getHighlightedNeighbors() != null) {
            for (Hex neighbor : gameState.getHighlightedNeighbors()) {
                Hexagon neighborHex = getHexagonAt(neighbor.getX(), neighbor.getY());
                if (neighborHex != null && neighbor.getOwnerId() == -1) {
                    neighborHex.setHighlighted(true);
                }
            }
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
}