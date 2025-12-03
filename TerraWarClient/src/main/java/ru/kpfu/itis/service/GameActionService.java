package ru.kpfu.itis.service;

import lombok.RequiredArgsConstructor;
import ru.kpfu.itis.model.*;

import java.util.*;

@RequiredArgsConstructor
public class GameActionService {

    private final GameMap gameMap;
    private final GameMapService gameMapService;
    private final UnitManager unitManager;
    private final FarmManager farmManager;
    private final Game game;

    public List<Hex> calculateActionRadius(Unit unit) {
        List<Hex> actionArea = new ArrayList<>();

        if (!unit.canAct()) {
            return actionArea;
        }

        int radius = unit.getActionRadius();
        int ownerId = unit.getOwnerId();
        int unitLevel = unit.getLevel();

        Queue<HexDistance> queue = new LinkedList<>();
        Map<String, Integer> visited = new HashMap<>();

        String startKey = unit.getHexX() + "," + unit.getHexY();
        queue.add(new HexDistance(unit.getHexX(), unit.getHexY(), 0));
        visited.put(startKey, 0);

        Set<String> blockedHexes = findBlockedHexes(unit);

        while (!queue.isEmpty()) {
            HexDistance current = queue.poll();

            if (current.distance > 0) {
                String hexKey = current.x + "," + current.y;

                if (blockedHexes.contains(hexKey)) {
                    continue;
                }

                Hex currentHex = gameMap.getHex(current.x, current.y);

                if (currentHex != null) {
                    Unit unitOnTarget = unitManager.getUnitAt(current.x, current.y);

                    Farm farmOnTarget = farmManager.getFarmAt(current.x, current.y);

                    if (farmOnTarget != null && farmOnTarget.getOwnerId() == ownerId) {
                        continue;
                    }

                    boolean isOwnTerritory = currentHex.getOwnerId() == ownerId;
                    boolean isEnemyTerritory = currentHex.getOwnerId() != -1 &&
                            currentHex.getOwnerId() != ownerId;
                    boolean isNeutralTerritory = currentHex.getOwnerId() == -1;

                    boolean canMove = false;

                    if (isOwnTerritory && current.distance <= radius) {
                        canMove = true;
                    }

                    if ((isEnemyTerritory || isNeutralTerritory) && current.distance == 1) {
                        canMove = true;
                    }

                    if (canMove && (unitOnTarget == null || unitOnTarget.getOwnerId() != ownerId)) {
                        actionArea.add(currentHex);
                    }
                }
            }

            if (current.distance < radius) {
                Hex currentHex = gameMap.getHex(current.x, current.y);
                if (currentHex != null) {
                    boolean currentIsOwn = currentHex.getOwnerId() == ownerId;

                    if (currentIsOwn) {
                        List<Hex> neighbors = gameMapService.getNeighbors(current.x, current.y);

                        for (Hex neighbor : neighbors) {
                            String key = neighbor.getX() + "," + neighbor.getY();

                            if (!visited.containsKey(key)) {
                                visited.put(key, current.distance + 1);
                                queue.add(new HexDistance(
                                        neighbor.getX(),
                                        neighbor.getY(),
                                        current.distance + 1
                                ));
                            }
                        }
                    }
                }
            }
        }

        return actionArea;
    }

    private Set<String> findBlockedHexes(Unit unit) {
        Set<String> blockedHexes = new HashSet<>();
        int unitLevel = unit.getLevel();
        int ownerId = unit.getOwnerId();

        for (int opponentId = 0; opponentId < 2; opponentId++) {
            if (opponentId == ownerId) continue;

            List<Unit> opponentUnits = unitManager.getPlayerUnits(opponentId);

            for (Unit opponentUnit : opponentUnits) {
                int opponentLevel = opponentUnit.getLevel();

                if (opponentLevel > unitLevel) {
                    List<Hex> neighborHexes = gameMapService.getNeighbors(
                            opponentUnit.getHexX(),
                            opponentUnit.getHexY()
                    );

                    for (Hex neighborHex : neighborHexes) {
                        String hexKey = neighborHex.getX() + "," + neighborHex.getY();
                        blockedHexes.add(hexKey);
                    }
                }
            }
        }

        return blockedHexes;
    }

    public boolean isHexInRadius(List<Hex> actionHexes, int hexX, int hexY) {
        if (actionHexes == null) {
            return false;
        }
        return actionHexes.stream()
                .anyMatch(hex -> hex.getX() == hexX && hex.getY() == hexY);
    }


    public boolean actWithUnit(Unit actingUnit, int targetHexX, int targetHexY) {
        if (actingUnit == null || !actingUnit.canAct()) {
            return false;
        }

        Hex targetHex = gameMap.getHex(targetHexX, targetHexY);
        if (targetHex == null) {
            return false;
        }

        Farm targetFarm = farmManager.getFarmAt(targetHexX, targetHexY);
        if (targetFarm != null) {
            if (targetFarm.getOwnerId() == actingUnit.getOwnerId()) {
                return false;
            }
        }

        Unit targetUnit = unitManager.getUnitAt(targetHexX, targetHexY);

        if (targetFarm != null && targetFarm.getOwnerId() != actingUnit.getOwnerId()) {
            farmManager.removeFarm(targetFarm.getId());
        }

        if (targetUnit != null) {
            if (targetUnit.getOwnerId() != actingUnit.getOwnerId()) {
                if (actingUnit.canDefeat(targetUnit)) {
                    unitManager.removeUnit(targetUnit.getId());
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        captureTerritory(actingUnit, targetHex);
        moveUnit(actingUnit, targetHexX, targetHexY);

        actingUnit.act();

        return true;
    }

    public void captureTerritory(Unit unit, Hex targetHex) {
        int previousOwnerId = targetHex.getOwnerId();

        if (targetHex.getOwnerId() != unit.getOwnerId()) {
            targetHex.setOwnerId(unit.getOwnerId());

            Player currentPlayer = game.getCurrentPlayer();
            if (currentPlayer != null) {
                currentPlayer.setBaseIncome(currentPlayer.getBaseIncome() + 1);
                currentPlayer.setIncome(currentPlayer.getBaseIncome() - currentPlayer.getUnitUpkeep());

                if (previousOwnerId != -1 && previousOwnerId != unit.getOwnerId()) {
                    for (int i = 0; i < 2; i++) {
                        if (i == previousOwnerId) {
                            Player enemyPlayer = null;
                            if (i == 0) {
                                enemyPlayer = game.getPlayers().size() > 0 ? game.getPlayers().get(0) : null;
                            } else {
                                enemyPlayer = game.getPlayers().size() > 1 ? game.getPlayers().get(1) : null;
                            }

                            if (enemyPlayer != null) {
                                enemyPlayer.setBaseIncome(enemyPlayer.getBaseIncome() - 1);
                                enemyPlayer.setIncome(enemyPlayer.getBaseIncome() - enemyPlayer.getUnitUpkeep());
                            }
                            break;
                        }
                    }
                } else {
                    // Захват нейтральной территории
                }
            }
        }
    }

    private void moveUnit(Unit unit, int targetX, int targetY) {
        unit.setHexX(targetX);
        unit.setHexY(targetY);
    }

    private static class HexDistance {
        int x, y, distance;

        HexDistance(int x, int y, int distance) {
            this.x = x;
            this.y = y;
            this.distance = distance;
        }
    }
}