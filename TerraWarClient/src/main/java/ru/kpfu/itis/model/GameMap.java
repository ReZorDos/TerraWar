package ru.kpfu.itis.model;

import lombok.Getter;
import ru.kpfu.itis.enums.Type;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GameMap {

    private final int width;
    private final int height;
    private final List<List<Hex>> grid;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new ArrayList<>();
        initializeMap();
    }

    private void initializeMap() {
        for (int y = 0; y < height; y++) {
            List<Hex> row = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                row.add(new Hex(x, y, Type.GRASS));
            }
            grid.add(row);
        }
    }

    public Hex getHex(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid.get(y).get(x);
        }
        return null;
    }

    public Hex setHex(int x, int y, Hex hex) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid.get(y).set(x, hex);
        }
        return null;
    }

}
