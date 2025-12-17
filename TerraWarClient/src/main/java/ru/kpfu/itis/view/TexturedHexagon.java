package ru.kpfu.itis.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class TexturedHexagon extends Pane {
    public static final double SIZE = Hexagon.SIZE;
    private static final double OFFSET_X = 450;
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

        texWidth = SIZE * Math.sqrt(3) * 3;
        texHeight = SIZE * 3;

        this.setPrefSize(texWidth, texHeight);
        this.setMaxSize(texWidth, texHeight);
        this.setMinSize(texWidth, texHeight);
        this.setStyle("-fx-padding: 0;");

        textureCanvas = new Canvas(texWidth, texHeight);
        drawTextureOnCanvas();

        clipShape = createHexagonPolygon();
        clipShape.setTranslateX(texWidth / 2);
        clipShape.setTranslateY(texHeight / 2);
        this.setClip(clipShape);

        border = createHexagonPolygon();
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.TRANSPARENT);
        border.setStrokeWidth(0.0);
        border.setTranslateX(texWidth / 2);
        border.setTranslateY(texHeight / 2);

        this.getChildren().addAll(textureCanvas, border);

        positionAtGridCoords();
    }

    private void drawTextureOnCanvas() {
        GraphicsContext gc = textureCanvas.getGraphicsContext2D();
        if (baseTextureImage != null && !baseTextureImage.isError()) {
            gc.drawImage(baseTextureImage, 0, 0, texWidth, texHeight);
        } else {
            gc.setFill(Color.TAN);
            gc.fillRect(0, 0, texWidth, texHeight);
        }
    }

    private Polygon createHexagonPolygon() {
        Polygon polygon = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = 2 * Math.PI / 6 * (i + 0.5);
            double x = SIZE * Math.cos(angle);
            double y = SIZE * Math.sin(angle);
            polygon.getPoints().addAll(x, y);
        }
        return polygon;
    }

    public void positionAtGridCoords() {
        double[] center = Hexagon.getCenterCoords(gridX, gridY);
        this.setTranslateX(center[0] + OFFSET_X - texWidth / 2);
        this.setTranslateY(center[1] + OFFSET_Y - texHeight / 2);
    }

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


    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        if (highlighted) {
            border.setStroke(Color.LIMEGREEN);
            border.setStrokeWidth(2.5);
        } else {
            if (selected) {
                border.setStroke(Color.YELLOW);
                border.setStrokeWidth(3.0);
            } else {
                border.setStroke(Color.TRANSPARENT);
                border.setStrokeWidth(0.0);
            }
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
            border.setStroke(Color.TRANSPARENT);
            border.setStrokeWidth(0.0);
        }
    }

    public void setStroke(Color color) {
        border.setStroke(color);
    }

    public void setStrokeWidth(double width) {
        border.setStrokeWidth(width);
    }
}