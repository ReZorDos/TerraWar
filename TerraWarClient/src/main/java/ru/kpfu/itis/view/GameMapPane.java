package ru.kpfu.itis.view;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Hex;
import ru.kpfu.itis.service.GameMapService;
import ru.kpfu.itis.state.GameState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameMapPane extends Pane {

    private final GameMap gameMap;
    private final GameMapService gameMapService;
    private final GameState gameState;
    private final Map<String, Hexagon> hexagons;

    public GameMapPane(GameMap gameMap, GameMapService gameMapService) {
        this.gameMap = gameMap;
        this.gameMapService = gameMapService;
        this.gameState = new GameState();
        this.hexagons = new HashMap<>();
        initializeMap();
        setupEventHandlers();
    }

    private void initializeMap() {
        getChildren().clear();
        hexagons.clear();

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hexagon hexagon = new Hexagon(x, y);
                hexagon.positionAtGridCoords();

                Hex hexData = gameMap.getHex(x, y);
                updateHexagonAppearance(hexagon, hexData);

                String key = x + "," + y;
                hexagons.put(key, hexagon);
                getChildren().add(hexagon);
            }
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
        setOnMouseClicked(event -> {
            Hexagon clickedHex = getHexAtPixel(event.getX(), event.getY());
            if (clickedHex != null) {
                handleHexClick(clickedHex);
            }
        });
    }

    private void handleHexClick(Hexagon clickedHex) {
        Hex clickedHexData = gameMap.getHex(clickedHex.getGridX(), clickedHex.getGridY());

        if (clickedHexData.getOwnerId() != -1) {
            selectHex(clickedHex);
        } else if (gameState.isHighlightedNeighbor(clickedHex)) {
            colorNeighbor(clickedHex);
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

    private void colorNeighbor(Hexagon neighborHex) {
        Hex neighborData = gameMap.getHex(neighborHex.getGridX(), neighborHex.getGridY());
        neighborData.setOwnerId(gameState.getSelectedOwnerId());
        updateHexagonAppearance(neighborHex, neighborData);
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