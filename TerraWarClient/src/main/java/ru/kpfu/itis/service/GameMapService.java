package ru.kpfu.itis.service;

import lombok.RequiredArgsConstructor;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Hex;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class GameMapService {

    private final GameMap gameMap;

    public List<Hex> getNeighbors(int x, int y) {
        List<Hex> neighbors = new ArrayList<>();

        int[][] directions;
        if (y % 2 == 0) {
            directions = new int[][]{
                    {1, 0},
                    {0, 1},
                    {-1, 1},
                    {-1, 0},
                    {-1, -1},
                    {0, -1}
            };
        } else {
            directions = new int[][]{
                    {1, 0},
                    {1, 1},
                    {0, 1},
                    {-1, 0},
                    {0, -1},
                    {1, -1}
            };
        }

        for (int[] dir : directions) {
            Hex neighbor = gameMap.getHex(x + dir[0], y + dir[1]);
            if (neighbor != null) {
                neighbors.add(neighbor);
            }
        }

        return neighbors;
    }

}
