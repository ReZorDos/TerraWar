package ru.kpfu.itis.view;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import ru.kpfu.itis.model.*;
import ru.kpfu.itis.service.FarmManager;
import ru.kpfu.itis.service.Game;
import ru.kpfu.itis.service.TowerManager;
import ru.kpfu.itis.service.UnitManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for rendering map hexes and objects (units, farms, towers) on the pane.
 */
public class MapRenderer {

    private final GameMap gameMap;
    private final Game game;
    private final UnitManager unitManager;
    private final FarmManager farmManager;
    private final TowerManager towerManager;
    private final Pane mapPane;
    private final Map<String, Hexagon> hexagons = new HashMap<>();
    private final ImageCache imageCache;

    public MapRenderer(GameMap gameMap,
                       Game game,
                       UnitManager unitManager,
                       FarmManager farmManager,
                       TowerManager towerManager,
                       Pane mapPane,
                       ImageCache imageCache) {
        this.gameMap = gameMap;
        this.game = game;
        this.unitManager = unitManager;
        this.farmManager = farmManager;
        this.towerManager = towerManager;
        this.mapPane = mapPane;
        this.imageCache = imageCache;
    }

    public void initializeMap() {
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

    public Hexagon getHexagonAt(int gridX, int gridY) {
        return hexagons.get(gridKey(gridX, gridY));
    }

    public Hexagon getHexAtPixel(double mouseX, double mouseY) {
        return hexagons.values().stream()
                .filter(hexagon -> hexagon.getBoundsInParent().contains(mouseX, mouseY))
                .findFirst()
                .orElse(null);
    }

    public Map<String, Hexagon> getHexagons() {
        return hexagons;
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

    private String gridKey(int x, int y) {
        return x + "," + y;
    }

    private void drawUnits() {
        mapPane.getChildren().removeIf(node ->
                (node instanceof StackPane &&
                        node.getUserData() != null &&
                        ((String) node.getUserData()).startsWith("UNIT_STACK_"))
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
                        node.getUserData() != null &&
                        ((String) node.getUserData()).startsWith("FARM_STACK_"))
        );

        for (Player player : game.getPlayers()) {
            List<Farm> farms = farmManager.getPlayerFarms(player.getId());
            for (Farm farm : farms) {
                drawFarmImageWithSymbol(farm);
            }
        }
    }

    private void drawFarmImageWithSymbol(Farm farm) {
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
                        node.getUserData() != null &&
                        ((String) node.getUserData()).startsWith("TOWER_STACK_"))
        );

        for (Player player : game.getPlayers()) {
            List<Tower> towers = towerManager.getPlayerTowers(player.getId());
            for (Tower tower : towers) {
                drawTowerImageWithSymbol(tower);
            }
        }
    }

    private void drawTowerImageWithSymbol(Tower tower) {
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
}

