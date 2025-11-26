package ru.kpfu.itis.service;

import lombok.RequiredArgsConstructor;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Hex;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.model.Unit;

import java.util.*;

/**
 * Сервис для обработки всех игровых действий:
 * - Вычисление радиуса действия с новыми правилами
 * - Логика боя и атаки
 * - Перемещение юнитов
 * - Захват территории
 *
 * ОБНОВЛЕННЫЕ ПРАВИЛА (v2.2):
 * 1. Юнит БЛОКИРУЕТСЯ враждебными юнитами ВЫШЕ его уровня
 *    - Пехота (ур.1) блокируется: Конницей и Артиллерией врага
 *    - Конница (ур.2) блокируется: Артиллерией врага
 *    - Артиллерия (ур.3) не блокируется никем
 *
 * 2. На своей территории - радиус зависит от уровня (1, 2 или 3)
 * 3. Новую территорию - захват только на 1 гекс (всегда соседний)
 * 4. ✨ ПРИ ЗАХВАТЕ враг теряет 1 доход, мы получаем +1 доход
 */
@RequiredArgsConstructor
public class GameActionService {

    private final GameMap gameMap;
    private final GameMapService gameMapService;
    private final UnitManager unitManager;
    private final Game game;

    /**
     * Вычисляет список гексов, доступных для действия юнита (BFS алгоритм)
     * с новыми правилами ограничения
     */
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

        // Первый проход: найти все враждебные юниты, которые блокируют этого юнита
        Set<String> blockedHexes = findBlockedHexes(unit);

        while (!queue.isEmpty()) {
            HexDistance current = queue.poll();

            if (current.distance > 0) {
                String hexKey = current.x + "," + current.y;

                // Проверяем, заблокирован ли этот гекс враждебным юнитом
                if (blockedHexes.contains(hexKey)) {
                    continue;  // Пропускаем заблокированные гексы
                }

                Hex currentHex = gameMap.getHex(current.x, current.y);

                if (currentHex != null) {
                    Unit unitOnTarget = unitManager.getUnitAt(current.x, current.y);

                    // ПРАВИЛО 2 & 3: Определяем условия добавления гекса
                    boolean isOwnTerritory = currentHex.getOwnerId() == ownerId;
                    boolean isEnemyTerritory = currentHex.getOwnerId() != -1 &&
                            currentHex.getOwnerId() != ownerId;
                    boolean isNeutralTerritory = currentHex.getOwnerId() == -1;

                    boolean canMove = false;

                    // На своей территории - можем ходить на радиус уровня
                    if (isOwnTerritory && current.distance <= radius) {
                        canMove = true;
                    }

                    // На новую территорию (враг или нейтраль) - только 1 гекс от границы
                    if ((isEnemyTerritory || isNeutralTerritory) && current.distance == 1) {
                        canMove = true;
                    }

                    if (canMove &&
                            (unitOnTarget == null || unitOnTarget.getOwnerId() != ownerId)) {
                        actionArea.add(currentHex);
                    }
                }
            }

            // Расширяем поиск на соседей ТОЛЬКО если находимся на своей территории
            if (current.distance < radius) {
                Hex currentHex = gameMap.getHex(current.x, current.y);
                if (currentHex != null) {
                    boolean currentIsOwn = currentHex.getOwnerId() == ownerId;

                    // Продолжаем поиск, только если на своей территории
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

    /**
     * Находит все гексы, которые БЛОКИРОВАНЫ враждебными юнитами
     *
     * Правило блокировки:
     * - Пехота (ур.1) блокируется: Конницей (ур.2) и Артиллерией (ур.3) врага
     * - Конница (ур.2) блокируется: Артиллерией (ур.3) врага
     * - Артиллерия (ур.3) не блокируется
     */
    private Set<String> findBlockedHexes(Unit unit) {
        Set<String> blockedHexes = new HashSet<>();
        int unitLevel = unit.getLevel();
        int ownerId = unit.getOwnerId();

        // Получаем всех юнитов противника
        for (int opponentId = 0; opponentId < 2; opponentId++) {
            if (opponentId == ownerId) continue;  // Пропускаем своих

            List<Unit> opponentUnits = unitManager.getPlayerUnits(opponentId);

            for (Unit opponentUnit : opponentUnits) {
                int opponentLevel = opponentUnit.getLevel();

                // Проверяем, может ли враг блокировать нашего юнита
                if (opponentLevel > unitLevel) {
                    // Враг блокирует! Добавляем все соседние гексы в blockedHexes
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
                            " (ур. " + actingUnit.getLevel() + ") уничтожил " +
                            targetUnit.getUnitTypeName() + " (ур. " + targetUnit.getLevel() + ")");
                } else {
                    System.out.println("[BATTLE] Нельзя атаковать: враг сильнее! " +
                            "Ваш уровень: " + actingUnit.getLevel() +
                            ", враг: " + targetUnit.getLevel());
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
     * ✨ v2.2: Захватывает территорию и изменяет доход обоих игроков
     * - Мы получаем +1 доход
     * - Враг теряет -1 доход
     */
    private void captureTerritory(Unit unit, Hex targetHex) {
        int previousOwnerId = targetHex.getOwnerId();

        if (targetHex.getOwnerId() != unit.getOwnerId()) {
            targetHex.setOwnerId(unit.getOwnerId());

            // Получаем текущего игрока (атакующего)
            Player currentPlayer = game.getCurrentPlayer();
            if (currentPlayer != null) {
                // НОВОЕ (v2.2): Увеличиваем доход текущего игрока
                currentPlayer.setIncome(currentPlayer.getIncome() + 1);

                // НОВОЕ (v2.2): Если захватили вражескую территорию, враг теряет 1 доход
                if (previousOwnerId != -1 && previousOwnerId != unit.getOwnerId()) {
                    // Находим врага и уменьшаем его доход
                    for (int i = 0; i < 2; i++) {
                        if (i == previousOwnerId) {
                            // Это враг, чью территорию мы захватили
                            Player enemyPlayer = null;
                            if (i == 0) {
                                enemyPlayer = game.getPlayers().size() > 0 ? game.getPlayers().get(0) : null;
                            } else {
                                enemyPlayer = game.getPlayers().size() > 1 ? game.getPlayers().get(1) : null;
                            }

                            if (enemyPlayer != null) {
                                int newIncome = Math.max(0, enemyPlayer.getIncome() - 1);
                                enemyPlayer.setIncome(newIncome);

                                System.out.println("[INCOME] Доход " + enemyPlayer.getName() +
                                        " уменьшен на 1 (до: " + newIncome + ")");
                            }
                            break;
                        }
                    }

                    System.out.println("[CAPTURE] " + unit.getUnitTypeName() +
                            " захватил вражескую территорию гекс (" + targetHex.getX() +
                            ", " + targetHex.getY() + ") у " + enemyPlayerName(previousOwnerId));
                } else {
                    // Захват нейтральной территории
                    System.out.println("[CAPTURE] " + unit.getUnitTypeName() +
                            " захватил нейтральный гекс (" + targetHex.getX() +
                            ", " + targetHex.getY() + ")");
                }

                System.out.println("[INCOME] Доход " + currentPlayer.getName() +
                        " увеличен до: " + currentPlayer.getIncome());
            }
        }
    }

    /**
     * Вспомогательный метод для получения имени врага по ID
     */
    private String enemyPlayerName(int playerId) {
        List<Player> players = game.getPlayers();
        if (playerId >= 0 && playerId < players.size()) {
            return players.get(playerId).getName();
        }
        return "враг";
    }

    /**
     * Перемещает юнита на новый гекс
     */
    private void moveUnit(Unit unit, int targetX, int targetY) {
        unit.setHexX(targetX);
        unit.setHexY(targetY);
        System.out.println("[MOVE] " + unit.getUnitTypeName() + " (ур. " + unit.getLevel() +
                ") переместился на гекс (" + targetX + ", " + targetY + ")");
    }

    /**
     * Вспомогательный класс для BFS с расстоянием
     */
    private static class HexDistance {
        int x, y, distance;

        HexDistance(int x, int y, int distance) {
            this.x = x;
            this.y = y;
            this.distance = distance;
        }
    }
}