package ru.kpfu.itis.service;

import ru.kpfu.itis.model.Tower;

public class TowerShop {
    private final TowerManager towerManager;

    public TowerShop(TowerManager towerManager) {
        this.towerManager = towerManager;
    }

    public Tower purchaseTower(int playerId, int hexX, int hexY, int level) {
        return towerManager.createTower(playerId, hexX, hexY, level);
    }

    public int getTowerPrice(int playerId, int level) {
        return towerManager.getTowerPrice(playerId, level);
    }

    public boolean canAffordTower(int playerMoney, int level, int playerId) {
        return towerManager.canAffordTower(playerMoney, level, playerId);
    }
}
