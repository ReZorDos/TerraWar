package ru.kpfu.itis.view;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import ru.kpfu.itis.model.Farm;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Hex;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.model.Tower;
import ru.kpfu.itis.model.Unit;
import ru.kpfu.itis.service.FarmManager;
import ru.kpfu.itis.service.Game;
import ru.kpfu.itis.service.TowerManager;
import ru.kpfu.itis.service.UnitManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for rendering map hexes and objects (units, farms, towers) on the pane.
 * FIXED: Убрали overlay и клик-гексы
 */
public class MapRenderer {
    private final GameMap gameMap;
    private final Game game;
    private final UnitManager unitManager;
    private final FarmManager farmManager;
    private final TowerManager towerManager;
    private final Pane mapPane;
    private final Map<String, TexturedHexagon> hexagons = new HashMap<>();
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
                Hex hexData = gameMap.getHex(x, y);

                // ✅ Если гекса нет (null), используем текстуру моря
                Image baseTexture;
                boolean isSea = false;

                if (hexData == null) {
                    // Пустая клетка = море
                    baseTexture = imageCache.get("sea_texture");
                    isSea = true;
                } else {
                    // Обычный гекс
                    baseTexture = getTextureForHex(hexData);
                }

                if (baseTexture == null) {
                    baseTexture = new WritableImage(1, 1);
                }

                TexturedHexagon hexagon = new TexturedHexagon(x, y, baseTexture);

                // ✅ Если это море - убираем границы
                if (isSea) {
                    hexagon.setStroke(Color.TRANSPARENT);
                    hexagon.setStrokeWidth(0);
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

    public TexturedHexagon getHexagonAt(int gridX, int gridY) {
        return hexagons.get(gridKey(gridX, gridY));
    }

    public TexturedHexagon getHexAtPixel(double mouseX, double mouseY) {
        return hexagons.values().stream()
                .filter(hexagon -> hexagon.getBoundsInParent().contains(mouseX, mouseY))
                .findFirst()
                .orElse(null);
    }

    public Map<String, TexturedHexagon> getHexagons() {
        return hexagons;
    }

    private Image getTextureForHex(Hex hexData) {
        if (hexData.getOwnerId() == -1) {
            return imageCache.get("hex_desert"); // Нейтральные - пустыня
        }
        return switch (hexData.getOwnerId()) {
            case 0 -> imageCache.get("hex_grass_red");
            case 1 -> imageCache.get("hex_grass_blue");
            default -> imageCache.get("hex_desert");
        };
    }

    public void updateHexOwner(int x, int y, int ownerId) {
        TexturedHexagon hexagon = getHexagonAt(x, y);
        if (hexagon != null) {
            // Не используем overlay - они не загружаются
            hexagon.setOwner(ownerId, null, null);
        }
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
        TexturedHexagon hexagon = getHexagonAt(unit.getHexX(), unit.getHexY());
        if (hexagon == null) return;

        StackPane stackPane = new StackPane();
        stackPane.setUserData("UNIT_STACK_" + unit.getHexX() + "_" + unit.getHexY());
        stackPane.setPickOnBounds(false);

        Text levelText = new Text(String.valueOf(unit.getLevel()));
        levelText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        levelText.setFill(Color.WHITE);
        levelText.setStroke(Color.BLACK);
        levelText.setStrokeWidth(1.0);

        Image image = imageCache.get("unit_" + unit.getLevel());
        if (image != null) {
            ImageView unitImage = new ImageView(image);
            double imageSize = Hexagon.SIZE * 1.5;
            unitImage.setFitWidth(imageSize);
            unitImage.setFitHeight(imageSize);
            unitImage.setPreserveRatio(true);
            unitImage.setUserData("UNIT");

            stackPane.getChildren().add(unitImage);
            stackPane.getChildren().add(levelText);

            double[] center = hexagon.getActualCenter();
            double centerX = center[0];
            double centerY = center[1];

            double stackWidth = Hexagon.SIZE * 2;
            double stackHeight = Hexagon.SIZE * 2;
            stackPane.setPrefSize(stackWidth, stackHeight);

            stackPane.setLayoutX(centerX - stackWidth / 2);
            stackPane.setLayoutY(centerY - stackHeight / 2);

            mapPane.getChildren().add(stackPane);
        }
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
        TexturedHexagon hexagon = getHexagonAt(farm.getHexX(), farm.getHexY());
        if (hexagon == null) return;

        StackPane stackPane = new StackPane();
        stackPane.setUserData("FARM_STACK_" + farm.getHexX() + "_" + farm.getHexY());
        stackPane.setPickOnBounds(false);

        Image image = imageCache.get("farm");
        if (image != null) {
            ImageView farmImage = new ImageView(image);
            double imageSize = Hexagon.SIZE * 1.2;
            farmImage.setFitWidth(imageSize);
            farmImage.setFitHeight(imageSize);
            farmImage.setPreserveRatio(true);

            stackPane.getChildren().add(farmImage);

            double[] center = hexagon.getActualCenter();
            stackPane.setPrefSize(imageSize, imageSize);
            stackPane.setLayoutX(center[0] - imageSize / 2);
            stackPane.setLayoutY(center[1] - imageSize / 2);

            mapPane.getChildren().add(stackPane);
        }
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
        TexturedHexagon hexagon = getHexagonAt(tower.getHexX(), tower.getHexY());
        if (hexagon == null) return;

        StackPane stackPane = new StackPane();
        stackPane.setUserData("TOWER_STACK_" + tower.getHexX() + "_" + tower.getHexY());
        stackPane.setPickOnBounds(false);

        Image image = imageCache.get("tower_" + tower.getLevel());
        if (image != null) {
            ImageView towerImage = new ImageView(image);
            double imageSize = Hexagon.SIZE * 1.4;
            towerImage.setFitWidth(imageSize);
            towerImage.setFitHeight(imageSize);
            towerImage.setPreserveRatio(true);

            stackPane.getChildren().add(towerImage);

            double[] center = hexagon.getActualCenter();
            stackPane.setPrefSize(imageSize, imageSize);
            stackPane.setLayoutX(center[0] - imageSize / 2);
            stackPane.setLayoutY(center[1] - imageSize / 2);

            mapPane.getChildren().add(stackPane);
        }
    }
}