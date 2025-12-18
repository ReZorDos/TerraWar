package ru.kpfu.itis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.model.*;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
public class GameActionService {

    private final GameMap gameMap;
    private final GameMapService gameMapService;
    private final UnitManager unitManager;
    private final FarmManager farmManager;
    private final Game game;
    private final TowerManager towerManager;

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

        for (Player player : game.getPlayers()) {
            int opponentId = player.getId();
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

        blockedHexes.addAll(towerManager.getAllBlockedHexesByTowers(ownerId, unitLevel));

        return blockedHexes;
    }

    public String canUnitMoveToHex(Unit unit, Hex targetHex) {
        Tower tower = towerManager.getTowerAt(targetHex.getX(), targetHex.getY());

        if (tower != null && tower.getOwnerId() != unit.getOwnerId()) {
            if (!tower.canUnitPassThrough(unit.getLevel())) {
                if (tower.getLevel() == 1) {
                    return "Клетка защищена башней 1 уровня. Юнит уровня " + unit.getLevel() + " не может пройти!";
                } else if (tower.getLevel() == 2) {
                    return "Клетка защищена башней 2 уровня. Юнит уровня " + unit.getLevel() + " не может пройти!";
                }
            }
        }
        return null;
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

        String blockReason = canUnitMoveToHex(actingUnit, targetHex);
        if (blockReason != null) {
            log.info(blockReason);
            return false;
        }

        Farm targetFarm = farmManager.getFarmAt(targetHexX, targetHexY);
        if (targetFarm != null) {
            if (targetFarm.getOwnerId() == actingUnit.getOwnerId()) {
                return false;
            }
        }

        Tower targetTower = towerManager.getTowerAt(targetHexX, targetHexY);
        if (targetTower != null && targetTower.getOwnerId() != actingUnit.getOwnerId()) {
            if (!targetTower.canUnitAttack(actingUnit.getLevel())) {
                return false;
            }
            towerManager.removeTower(targetTower.getId());
            captureTerritory(actingUnit, targetHex);
            moveUnit(actingUnit, targetHexX, targetHexY);
            actingUnit.act();
            return true;
        }

        if (targetTower != null && targetTower.getOwnerId() == actingUnit.getOwnerId()) {
            return false;
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
            }

            if (previousOwnerId != -1 && previousOwnerId != unit.getOwnerId()) {
                Player enemyPlayer = game.getPlayers().stream()
                        .filter(p -> p.getId() == previousOwnerId)
                        .findFirst()
                        .orElse(null);

                if (enemyPlayer != null) {
                    enemyPlayer.setBaseIncome(Math.max(0, enemyPlayer.getBaseIncome() - 1));
                    enemyPlayer.setIncome(enemyPlayer.getBaseIncome() - enemyPlayer.getUnitUpkeep() - enemyPlayer.getTowerUpkeep() + enemyPlayer.getFarmIncome());
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