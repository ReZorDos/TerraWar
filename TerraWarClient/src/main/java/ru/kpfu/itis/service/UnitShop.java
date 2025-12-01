package ru.kpfu.itis.service;

import ru.kpfu.itis.model.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnitShop {

    private final Map<Integer, Integer> unitPrices;

    public UnitShop() {
        unitPrices = new HashMap<>();
        unitPrices.put(1, 10);
        unitPrices.put(2, 25);
        unitPrices.put(3, 40);
    }

    public int getUnitPrice(int level) {
        return unitPrices.getOrDefault(level, Integer.MAX_VALUE);
    }

    public boolean canAffordUnit(int playerMoney, int level) {
        return playerMoney >= getUnitPrice(level);
    }

    public Unit purchaseUnit(UnitManager unitManager, int ownerId, int hexX, int hexY, int level) {
        return unitManager.createUnit(ownerId, hexX, hexY, level);
    }

}