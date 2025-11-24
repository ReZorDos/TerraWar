package ru.kpfu.itis.service;

import lombok.Getter;
import ru.kpfu.itis.model.Unit;
import java.util.*;

@Getter
public class UnitManager {
    private final Map<Integer, List<Unit>> playerUnits;
    private int unitIdCounter = 0;

    public UnitManager() {
        this.playerUnits = new HashMap<>();
    }

    public Unit createUnit(int ownerId, int hexX, int hexY, int level) {
        Unit unit = new Unit(unitIdCounter++, ownerId, hexX, hexY, level);
        playerUnits.putIfAbsent(ownerId, new ArrayList<>());
        playerUnits.get(ownerId).add(unit);
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
        for (List<Unit> units : playerUnits.values()) {
            units.removeIf(unit -> unit.getId() == unitId);
        }
    }

    public boolean hasUnits(int playerId) {
        return !getPlayerUnits(playerId).isEmpty();
    }
}