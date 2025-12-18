package ru.kpfu.itis.view;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import lombok.Getter;

@Getter
public class Hexagon extends Polygon {

    public static final double SIZE = 25.0;
    private final int gridX;
    private final int gridY;
    private boolean highlighted = false;
    private boolean selected = false;

    public Hexagon(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
        createHexagonShape();
        setStroke(Color.BLACK);
        setStrokeWidth(1.0);
        setFill(Color.LIGHTGRAY);
        setPickOnBounds(true);
    }

    private void createHexagonShape() {
        getPoints().clear();
        for (int i = 0; i < 6; i++) {
            double angle = 2 * Math.PI / 6 * (i + 0.5);
            double x = SIZE * Math.cos(angle);
            double y = SIZE * Math.sin(angle);
            getPoints().addAll(x, y);
        }
    }

    public static double[] getCenterCoords(int gridX, int gridY) {
        double hexWidth = SIZE * Math.sqrt(3);
        double hexHeight = SIZE * 2;
        double centerX = gridX * hexWidth;
        double centerY = gridY * hexHeight * 0.75;
        if (gridY % 2 == 1) {
            centerX += hexWidth / 2;
        }
        return new double[]{centerX, centerY};
    }

}