package ru.kpfu.itis.service;

import ru.kpfu.itis.model.Farm;

public class FarmShop {
    private final FarmManager farmManager;

    public FarmShop(FarmManager farmManager) {
        this.farmManager = farmManager;
    }

    public int getFarmPrice(int playerId) {
        return farmManager.getFarmPriceForPlayer(playerId);
    }

    public boolean canAffordFarm(int playerMoney, int playerId) {
        return playerMoney >= getFarmPrice(playerId);
    }

    public Farm purchaseFarm(int ownerId, int hexX, int hexY) {
        return farmManager.createFarm(ownerId, hexX, hexY);
    }
}