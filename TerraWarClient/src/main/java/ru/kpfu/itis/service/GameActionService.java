package ru.kpfu.itis.service;

import lombok.RequiredArgsConstructor;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Hex;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.model.Unit;

import java.util.*;

/**
 * Сервис для обработки всех игровых действий:
 * - Вычисление радиуса действия
 * - Логика боя и атаки
 * - Перемещение юнитов
 * - Захват территории
 */
@RequiredArgsConstructor
public class GameActionService {

    private final GameMap gameMap;
    private final GameMapService gameMapService;
    private final UnitManager unitManager;
    private final Game game;

    /**
     * Вычисляет список гексов, доступных для действия юнита
     */
    public List calculateActionRadius(Unit unit) {
        List actionArea = new ArrayList<>();

        if (!unit.canAct()) {
            return actionArea;
        }

        int radius = unit.getActionRadius();
        Queue<HexDistance> queue = new LinkedList<>();
        Map<String, Integer> visited = new HashMap<>();

        String startKey = unit.getHexX() + "," + unit.getHexY();
        queue.add(new HexDistance(unit.getHexX(), unit.getHexY(), 0));
        visited.put(startKey, 0);

        while (!queue.isEmpty()) {
            HexDistance current = queue.poll();

            if (current.distance > 0 && current.distance <= radius) {
                Hex currentHex = gameMap.getHex(current.x, current.y);

                if (currentHex != null) {
                    // Проверяем, свободна ли клетка или есть вражеский юнит
                    Unit unitOnTarget = unitManager.getUnitAt(current.x, current.y);

                    if (unitOnTarget == null ||
                            unitOnTarget.getOwnerId() != unit.getOwnerId()) {
                        actionArea.add(currentHex);
                    }
                }
            }

            if (current.distance < radius) {
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

        return actionArea;
    }

    /**
     * Проверяет, находится ли гекс в радиусе действия
     */
    public boolean isHexInRadius(List<Hex> actionHexes, int hexX, int hexY) {
        if (actionHexes == null) {
            return false;
        }
        return actionHexes.stream()
                .anyMatch(hex -> hex.getX() == hexX && hex.getY() == hexY);
    }

    /**
     * Выполняет действие юнита: боевое столкновение или захват территории
     *
     * @return true если действие выполнено успешно, false если не удалось
     */
    public boolean actWithUnit(Unit actingUnit, int targetHexX, int targetHexY) {
        if (actingUnit == null || !actingUnit.canAct()) {
            return false;
        }

        Hex targetHex = gameMap.getHex(targetHexX, targetHexY);
        if (targetHex == null) {
            return false;
        }

        Unit targetUnit = unitManager.getUnitAt(targetHexX, targetHexY);

        // Обработка боевого столкновения
        if (targetUnit != null) {
            if (targetUnit.getOwnerId() != actingUnit.getOwnerId()) {
                // Вражеский юнит - проверяем боевую силу
                if (actingUnit.canDefeat(targetUnit)) {
                    unitManager.removeUnit(targetUnit.getId());
                    System.out.println("[BATTLE] " + actingUnit.getUnitTypeName() +
                            " уничтожил вражеского юнита уровня " + targetUnit.getLevel());
                } else {
                    System.out.println("[BATTLE] Нельзя атаковать: уровень вражеского юнита выше");
                    return false;
                }
            } else {
                // Дружественный юнит - нельзя ходить
                System.out.println("[MOVE] Нельзя переместиться на клетку с дружественным юнитом");
                return false;
            }
        }

        // Захват территории
        captureTerritory(actingUnit, targetHex);

        // Перемещение юнита
        moveUnit(actingUnit, targetHexX, targetHexY);

        // Юнит завершил действие
        actingUnit.act();

        return true;
    }

    /**
     * Захватывает территорию для текущего игрока и увеличивает доход
     */
    private void captureTerritory(Unit unit, Hex targetHex) {
        if (targetHex.getOwnerId() != unit.getOwnerId()) {
            targetHex.setOwnerId(unit.getOwnerId());

            // Увеличиваем доход текущего игрока
            Player currentPlayer = game.getCurrentPlayer();
            if (currentPlayer != null) {
                currentPlayer.setIncome(currentPlayer.getIncome() + 1);
                System.out.println("[CAPTURE] " + unit.getUnitTypeName() +
                        " захватил гекс (" + targetHex.getX() + ", " + targetHex.getY() + ")");
                System.out.println("[INCOME] Доход " + currentPlayer.getName() +
                        " увеличен до: " + currentPlayer.getIncome());
            }
        }
    }

    /**
     * Перемещает юнита на новый гекс
     */
    private void moveUnit(Unit unit, int targetX, int targetY) {
        unit.setHexX(targetX);
        unit.setHexY(targetY);
        System.out.println("[MOVE] " + unit.getUnitTypeName() +
                " переместился на гекс (" + targetX + ", " + targetY + ")");
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