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
    private final PlayerService playerService;

    public UnitManager(Game game, PlayerService playerService) {
        this.playerUnits = new HashMap<>();
        this.game = game;
        this.playerService = playerService;
    }

    public Unit createUnit(int ownerId, int hexX, int hexY, int level) {
        Unit unit = new Unit(unitIdCounter++, ownerId, hexX, hexY, level);
        playerUnits.putIfAbsent(ownerId, new ArrayList<>());
        playerUnits.get(ownerId).add(unit);

        Player player = getPlayerById(unit.getOwnerId());
        if (player != null) {
            playerService.addUnitUpkeep(player, unit.getUpkeepCost());
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
                        playerService.removeUnitUpkeep(player, unit.getUpkeepCost());
                    }
                    iterator.remove();
                    return;
                }
            }
        }
    }

    public Player getPlayerById(int playerId) {
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
                playerService.removeUnitUpkeep(player, unit.getUpkeepCost());
            }
        }

        playerUnits.remove(playerId);
    }


}