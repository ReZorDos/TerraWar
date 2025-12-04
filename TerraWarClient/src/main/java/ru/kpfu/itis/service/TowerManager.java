package ru.kpfu.itis.service;

import lombok.Getter;
import ru.kpfu.itis.model.Tower;
import ru.kpfu.itis.model.Player;
import java.util.*;

@Getter
public class TowerManager {
    private final Map<Integer, List<Tower>> playerTowers;
    private int towerIdCounter = 0;
    private final Game game;
    private final PlayerService playerService;
    private final GameMapService gameMapService;

    private static final int TOWER_LEVEL_1_BASE_COST = 18;
    private static final int TOWER_LEVEL_2_BASE_COST = 40;

    private static final int TOWER_LEVEL_1_UPKEEP = 2;
    private static final int TOWER_LEVEL_2_UPKEEP = 4;

    public TowerManager(Game game, PlayerService playerService, GameMapService gameMapService) {
        this.playerTowers = new HashMap<>();
        this.game = game;
        this.playerService = playerService;
        this.gameMapService = gameMapService;
    }

    public Tower createTower(int ownerId, int hexX, int hexY, int level) {
        Tower tower = new Tower(towerIdCounter++, ownerId, hexX, hexY, level);
        playerTowers.putIfAbsent(ownerId, new ArrayList<>());
        playerTowers.get(ownerId).add(tower);

        Player owner = getPlayerById(ownerId);
        if (owner != null) {
            int upkeep = (level == 1) ? TOWER_LEVEL_1_UPKEEP : TOWER_LEVEL_2_UPKEEP;
            owner.setTowerUpkeep(owner.getTowerUpkeep() + upkeep);
            recalculatePlayerIncome(owner);
        }

        return tower;
    }

    public Tower getTowerAt(int hexX, int hexY) {
        for (List<Tower> towers : playerTowers.values()) {
            for (Tower tower : towers) {
                if (tower.getHexX() == hexX && tower.getHexY() == hexY) {
                    return tower;
                }
            }
        }
        return null;
    }

    public List<Tower> getPlayerTowers(int playerId) {
        return playerTowers.getOrDefault(playerId, new ArrayList<>());
    }

    public void removeTower(int towerId) {
        for (Map.Entry<Integer, List<Tower>> entry : playerTowers.entrySet()) {
            Iterator<Tower> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                Tower tower = iterator.next();
                if (tower.getId() == towerId) {
                    Player owner = getPlayerById(entry.getKey());
                    if (owner != null) {
                        int upkeep = (tower.getLevel() == 1) ? TOWER_LEVEL_1_UPKEEP : TOWER_LEVEL_2_UPKEEP;
                        owner.setTowerUpkeep(Math.max(0, owner.getTowerUpkeep() - upkeep));
                        recalculatePlayerIncome(owner);
                    }

                    iterator.remove();
                    return;
                }
            }
        }
    }

    private Player getPlayerById(int playerId) {
        return game.getPlayers().stream()
                .filter(player -> player.getId() == playerId)
                .findFirst()
                .orElse(null);
    }

    public int getTowerPrice(int playerId, int level) {
        Player player = getPlayerById(playerId);
        if (player == null) return Integer.MAX_VALUE;

        int baseCost = (level == 1) ? TOWER_LEVEL_1_BASE_COST : TOWER_LEVEL_2_BASE_COST;
        int towerCount = getPlayerTowers(playerId).size();

        return baseCost + (towerCount * 2);
    }

    public boolean canAffordTower(int playerMoney, int level, int playerId) {
        return playerMoney >= getTowerPrice(playerId, level);
    }

    public Set<String> getBlockedHexesByTower(Tower tower, int unitLevel) {
        Set<String> blockedHexes = new HashSet<>();

        if (tower.canUnitPassThrough(unitLevel)) {
            return blockedHexes;
        }

        List<ru.kpfu.itis.model.Hex> neighbors = gameMapService.getNeighbors(tower.getHexX(), tower.getHexY());

        for (ru.kpfu.itis.model.Hex neighbor : neighbors) {
            if (neighbor.getOwnerId() == tower.getOwnerId()) {
                String hexKey = neighbor.getX() + "," + neighbor.getY();
                blockedHexes.add(hexKey);
            }
        }

        return blockedHexes;
    }

    public Set<String> getAllBlockedHexesByTowers(int unitOwnerId, int unitLevel) {
        Set<String> allBlockedHexes = new HashSet<>();

        for (int opponentId = 0; opponentId < 2; opponentId++) {
            if (opponentId == unitOwnerId) continue;

            List<Tower> opponentTowers = getPlayerTowers(opponentId);
            for (Tower tower : opponentTowers) {
                Set<String> towerBlockedHexes = getBlockedHexesByTower(tower, unitLevel);
                allBlockedHexes.addAll(towerBlockedHexes);
            }
        }

        return allBlockedHexes;
    }

    private void recalculatePlayerIncome(Player player) {
        player.setIncome(player.getBaseIncome() - player.getUnitUpkeep() - player.getTowerUpkeep() + player.getFarmIncome());
    }
}