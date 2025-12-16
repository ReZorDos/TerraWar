package ru.kpfu.itis.view;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, TexturedHexagon> hexagons = new HashMap<>();
    private final ImageCache imageCache;
    // Храним StackPane для каждого юнита по его ID для анимации
    private final Map<Integer, StackPane> unitPanes = new HashMap<>();

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
        // Удаляем панели юнитов из сцены перед очисткой
        for (StackPane pane : unitPanes.values()) {
            mapPane.getChildren().remove(pane);
        }
        
        mapPane.getChildren().clear();
        hexagons.clear();
        unitPanes.clear(); // Очищаем полностью, юниты будут перерисованы

        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hex hexData = gameMap.getHex(x, y);

                // Если гекса нет (null), используем текстуру моря
                Image baseTexture;
                boolean isSea = false;
                if (hexData == null) {
                    baseTexture = imageCache.get("sea_texture");
                    isSea = true;
                } else {
                    baseTexture = getTextureForHex(hexData);
                }

                if (baseTexture == null) {
                    baseTexture = new WritableImage(1, 1);
                }

                TexturedHexagon hexagon = new TexturedHexagon(x, y, baseTexture);
                
                // Если это море - убираем границы
                if (isSea) {
                    hexagon.setStroke(Color.TRANSPARENT);
                    hexagon.setStrokeWidth(0);
                }

                String key = x + "," + y;
                hexagons.put(key, hexagon);
                mapPane.getChildren().add(hexagon);
            }
        }

        // Перерисовываем юнитов заново
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
        List<Unit> allUnits = unitManager.getAllUnits();
        Set<Integer> currentUnitIds = new HashSet<>();
        
        // Обрабатываем все юниты
        for (Unit unit : allUnits) {
            currentUnitIds.add(unit.getId());
            drawUnitImageWithNumber(unit);
        }
        
        // Удаляем панели юнитов, которых больше нет
        unitPanes.entrySet().removeIf(entry -> {
            if (!currentUnitIds.contains(entry.getKey())) {
                mapPane.getChildren().remove(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private void drawUnitImageWithNumber(Unit unit) {
        TexturedHexagon hexagon = getHexagonAt(unit.getHexX(), unit.getHexY());
        if (hexagon == null) {
            StackPane oldPane = unitPanes.remove(unit.getId());
            if (oldPane != null) {
                mapPane.getChildren().remove(oldPane);
            }
            return;
        }

        double[] center = hexagon.getActualCenter();
        double centerX = center[0];
        double centerY = center[1];

        double stackWidth = Hexagon.SIZE * 2;
        double stackHeight = Hexagon.SIZE * 2;
        double targetX = centerX - stackWidth / 2;
        double targetY = centerY - stackHeight / 2;

        StackPane stackPane = unitPanes.get(unit.getId());
        
        if (stackPane == null) {
            // Создаём новый StackPane для юнита
            stackPane = new StackPane();
            stackPane.setPickOnBounds(false);

            Text levelText = new Text(String.valueOf(unit.getLevel()));
            levelText.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            levelText.setFill(Color.WHITE);
            levelText.setStroke(Color.BLACK);
            levelText.setStrokeWidth(1.0);
            levelText.setUserData("UNIT_LEVEL_TEXT");

            Image image = imageCache.get("unit_" + unit.getLevel());
            if (image != null) {
                ImageView unitImage = new ImageView(image);
                double imageSize = Hexagon.SIZE * 1.5;
                unitImage.setFitWidth(imageSize);
                unitImage.setFitHeight(imageSize);
                unitImage.setPreserveRatio(true);
                unitImage.setUserData("UNIT_IMAGE");

                stackPane.getChildren().add(unitImage);
                stackPane.getChildren().add(levelText);
            }
            
            stackPane.setPrefSize(stackWidth, stackHeight);
            stackPane.setLayoutX(targetX);
            stackPane.setLayoutY(targetY);
            
            unitPanes.put(unit.getId(), stackPane);
            // Убеждаемся, что панель добавлена в mapPane
            if (!mapPane.getChildren().contains(stackPane)) {
                mapPane.getChildren().add(stackPane);
            }
        } else {
            // Юнит уже существует - обновляем уровень и анимируем перемещение
            
            // Обновляем уровень юнита, если он изменился
            Text levelText = (Text) stackPane.getChildren().stream()
                    .filter(node -> "UNIT_LEVEL_TEXT".equals(node.getUserData()))
                    .findFirst()
                    .orElse(null);
            if (levelText != null) {
                levelText.setText(String.valueOf(unit.getLevel()));
            }
            
            // Обновляем изображение юнита, если уровень изменился
            ImageView unitImage = (ImageView) stackPane.getChildren().stream()
                    .filter(node -> "UNIT_IMAGE".equals(node.getUserData()))
                    .findFirst()
                    .orElse(null);
            if (unitImage != null) {
                Image newImage = imageCache.get("unit_" + unit.getLevel());
                if (newImage != null && unitImage.getImage() != newImage) {
                    unitImage.setImage(newImage);
                }
            }
            
            // Убеждаемся, что панель всё ещё в mapPane
            if (!mapPane.getChildren().contains(stackPane)) {
                mapPane.getChildren().add(stackPane);
            }
            
            // Анимируем перемещение
            double currentX = stackPane.getLayoutX();
            double currentY = stackPane.getLayoutY();
            
            // Анимируем только если позиция действительно изменилась
            if (Math.abs(currentX - targetX) > 0.5 || Math.abs(currentY - targetY) > 0.5) {
                // Создаём плавную анимацию перемещения
                Timeline timeline = new Timeline();
                
                KeyFrame startFrame = new KeyFrame(
                    javafx.util.Duration.ZERO,
                    new KeyValue(stackPane.layoutXProperty(), currentX),
                    new KeyValue(stackPane.layoutYProperty(), currentY)
                );
                
                KeyFrame endFrame = new KeyFrame(
                    javafx.util.Duration.millis(800),
                    new KeyValue(stackPane.layoutXProperty(), targetX),
                    new KeyValue(stackPane.layoutYProperty(), targetY)
                );
                
                timeline.getKeyFrames().addAll(startFrame, endFrame);
                timeline.setCycleCount(1);
                timeline.play();
            } else {
                // Если позиция почти не изменилась, просто обновляем
                stackPane.setLayoutX(targetX);
                stackPane.setLayoutY(targetY);
            }
        }

        stackPane.setUserData("UNIT_STACK_" + unit.getHexX() + "_" + unit.getHexY());
    }

    private void drawFarms() {
        removeObjectsByPrefix("FARM_STACK_");
        for (Player player : game.getPlayers()) {
            for (Farm farm : farmManager.getPlayerFarms(player.getId())) {
                drawObject(farm.getHexX(), farm.getHexY(), "farm", "FARM_STACK_", Hexagon.SIZE * 1.2);
            }
        }
    }

    private void drawTowers() {
        removeObjectsByPrefix("TOWER_STACK_");
        for (Player player : game.getPlayers()) {
            for (Tower tower : towerManager.getPlayerTowers(player.getId())) {
                drawObject(tower.getHexX(), tower.getHexY(), "tower_" + tower.getLevel(), "TOWER_STACK_", Hexagon.SIZE * 1.4);
            }
        }
    }

    private void removeObjectsByPrefix(String prefix) {
        mapPane.getChildren().removeIf(node ->
                node instanceof StackPane &&
                node.getUserData() != null &&
                ((String) node.getUserData()).startsWith(prefix)
        );
    }

    private void drawObject(int hexX, int hexY, String imageKey, String userDataPrefix, double imageSize) {
        TexturedHexagon hexagon = getHexagonAt(hexX, hexY);
        if (hexagon == null) return;

        Image image = imageCache.get(imageKey);
        if (image == null) return;

        StackPane stackPane = new StackPane();
        stackPane.setUserData(userDataPrefix + hexX + "_" + hexY);
        stackPane.setPickOnBounds(false);

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(imageSize);
        imageView.setFitHeight(imageSize);
        imageView.setPreserveRatio(true);
        stackPane.getChildren().add(imageView);

        double[] center = hexagon.getActualCenter();
        stackPane.setPrefSize(imageSize, imageSize);
        stackPane.setLayoutX(center[0] - imageSize / 2);
        stackPane.setLayoutY(center[1] - imageSize / 2);

        mapPane.getChildren().add(stackPane);
    }
}