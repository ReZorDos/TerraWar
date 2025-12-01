package ru.kpfu.itis.service;

import lombok.Getter;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.model.Unit;
import java.util.*;

@Getter
public class UnitManager {
    private final Map<Integer, List<Unit>> playerUnits;
    private int unitIdCounter = 0;
    private final Game game;

    public UnitManager(Game game) {
        this.playerUnits = new HashMap<>();
        this.game = game;
    }

    public Unit createUnit(int ownerId, int hexX, int hexY, int level) {
        Unit unit = new Unit(unitIdCounter++, ownerId, hexX, hexY, level);
        playerUnits.putIfAbsent(ownerId, new ArrayList<>());
        playerUnits.get(ownerId).add(unit);

        Player player = getPlayerById(unit.getOwnerId());
        if (player != null) {
            player.addUnitUpkeep(unit.getUpkeepCost());
        }

        return unit;
    }

    public Unit getUnitAt(int hexX, int hexY) {
        for (List<Unit> units : playerUnits.values()) {
            for (Unit unit : units) {
                if (unit.getHexX() == hexX && unit.getHexY() == hexY) {
                    return unit;
                }
            }
        }
        return null;
    }

    public List<Unit> getPlayerUnits(int playerId) {
        return playerUnits.getOrDefault(playerId, new ArrayList<>());
    }

    public List<Unit> getAllUnits() {
        List<Unit> allUnits = new ArrayList<>();
        for (List<Unit> units : playerUnits.values()) {
            allUnits.addAll(units);
        }
        return allUnits;
    }

    public void removeUnit(int unitId) {
        for (Map.Entry<Integer, List<Unit>> entry : playerUnits.entrySet()) {
            Iterator<Unit> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                Unit unit = iterator.next();
                if (unit.getId() == unitId) {
                    Player player = getPlayerById(unit.getOwnerId());
                    if (player != null) {
                        player.removeUnitUpkeep(unit.getUpkeepCost());
                    }
                    iterator.remove();
                    return;
                }
            }
        }
    }

    // Вспомогательный метод для получения игрока по ID
    private Player getPlayerById(int playerId) {
        return game.getPlayers().stream()
                .filter(player -> player.getId() == playerId)
                .findFirst()
                .orElse(null);
    }


    public void removeAllPlayerUnits(int playerId) {
        List<Unit> unitsToRemove = playerUnits.getOrDefault(playerId, new ArrayList<>());
        Player player = getPlayerById(playerId);
        if (player != null) {
            for (Unit unit : unitsToRemove) {
                player.removeUnitUpkeep(unit.getUpkeepCost());
            }
        }

        playerUnits.remove(playerId);
    }


}