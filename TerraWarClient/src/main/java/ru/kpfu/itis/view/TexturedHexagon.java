package ru.kpfu.itis.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * Hexagon with textured background using Canvas + visible borders.
 * FIXED: Border is visible and positioned correctly at hexagon edges
 */
public class TexturedHexagon extends Pane {
    public static final double SIZE = Hexagon.SIZE;
    private static final double OFFSET_X = 230;
    private static final double OFFSET_Y = 120;

    private final int gridX;
    private final int gridY;
    private boolean highlighted = false;
    private boolean selected = false;

    private final Canvas textureCanvas;
    private final Polygon border;
    private final Polygon clipShape;

    private final double texWidth;
    private final double texHeight;

    private Image baseTextureImage;

    public TexturedHexagon(int gridX, int gridY, Image baseTextureImage) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.baseTextureImage = baseTextureImage;

        texWidth = SIZE * Math.sqrt(3) * 3;   // 129.9px
        texHeight = SIZE * 3;                  // 75px

        this.setPrefSize(texWidth, texHeight);
        this.setMaxSize(texWidth, texHeight);
        this.setMinSize(texWidth, texHeight);
        this.setStyle("-fx-padding: 0;");

        // === CANVAS для рисования текстуры ===
        textureCanvas = new Canvas(texWidth, texHeight);
        textureCanvas.setLayoutX(0);
        textureCanvas.setLayoutY(0);

        drawTextureOnCanvas();

        // === CLIP SHAPE (обрезаем по форме гекса) ===
        clipShape = createHexagonPolygon();
        clipShape.setTranslateX(texWidth / 2);
        clipShape.setTranslateY(texHeight / 2);
        this.setClip(clipShape);

        // === BORDER (чёрная граница гекса) ===
        border = createHexagonPolygon();
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.BLACK);
        border.setStrokeWidth(1.5);
        border.setTranslateX(texWidth / 2);
        border.setTranslateY(texHeight / 2);

        this.getChildren().addAll(textureCanvas, border);

        positionAtGridCoords();
    }

    /**
     * Рисует текстуру на Canvas - полностью заполняет его
     */
    private void drawTextureOnCanvas() {
        GraphicsContext gc = textureCanvas.getGraphicsContext2D();

        if (baseTextureImage != null && !baseTextureImage.isError()) {
            // ✅ Рисуем изображение, растягивая его на весь размер Canvas
            gc.drawImage(baseTextureImage, 0, 0, texWidth, texHeight);
        } else {
            // Fallback если картинка не загрузилась
            gc.setFill(Color.TAN);
            gc.fillRect(0, 0, texWidth, texHeight);
        }
    }

    private Polygon createHexagonPolygon() {
        Polygon polygon = new Polygon();
        // ✅ Вершины гекса центрированы в (0,0)
        for (int i = 0; i < 6; i++) {
            double angle = 2 * Math.PI / 6 * (i + 0.5);
            double x = SIZE * Math.cos(angle);
            double y = SIZE * Math.sin(angle);
            polygon.getPoints().addAll(x, y);
        }
        return polygon;
    }

    /**
     * Позиционирует Pane так, чтобы ЕГО ЦЕНТР совпадал с центром гекса
     */
    public void positionAtGridCoords() {
        double[] center = Hexagon.getCenterCoords(gridX, gridY);
        // ✅ Смещаем Pane так, чтобы его центр был в точке центра гекса
        this.setTranslateX(center[0] + OFFSET_X - texWidth / 2);
        this.setTranslateY(center[1] + OFFSET_Y - texHeight / 2);
    }

    /**
     * Возвращает истинный центр гекса (центр Pane)
     */
    public double[] getActualCenter() {
        return new double[]{
                getTranslateX() + texWidth / 2,
                getTranslateY() + texHeight / 2
        };
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public void setOwner(int ownerId, Image redOverlay, Image blueOverlay) {
        // Может быть использовано в будущем для оверлеев
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        if (highlighted) {
            border.setStroke(Color.LIMEGREEN);
            border.setStrokeWidth(2.5);
        } else {
            border.setStroke(selected ? Color.YELLOW : Color.BLACK);
            border.setStrokeWidth(selected ? 3.0 : 1.5);
        }
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            border.setStroke(Color.YELLOW);
            border.setStrokeWidth(3.0);
        } else if (highlighted) {
            border.setStroke(Color.LIMEGREEN);
            border.setStrokeWidth(2.5);
        } else {
            border.setStroke(Color.BLACK);
            border.setStrokeWidth(1.5);
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setStroke(Color color) {
        border.setStroke(color);
    }

    public void setStrokeWidth(double width) {
        border.setStrokeWidth(width);
    }
}