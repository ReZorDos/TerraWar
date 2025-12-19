package ru.kpfu.itis.service;

import ru.kpfu.itis.model.Farm;
import ru.kpfu.itis.model.Player;

import java.util.List;

public class PlayerService {

    public void addUnitUpkeep(Player player, int upkeep) {
        player.setUnitUpkeep(player.getUnitUpkeep() + upkeep);
        updateTotalIncome(player);
    }

    public void updateTotalIncome(Player player) {
        player.setIncome(player.getBaseIncome() - player.getUnitUpkeep() - player.getTowerUpkeep() + player.getFarmIncome());
    }

    public void addFarm(Player player, Farm farm) {
        List<Farm> farms = player.getFarms();
        farms.add(farm);
        player.setFarms(farms);
        player.setFarmIncome(player.getFarmIncome() + farm.getIncome());
        updateTotalIncome(player);
    }

    public void removeFarm(Player player, Farm farm) {
        if (player.getFarms().remove(farm)) {
            player.setFarmIncome(player.getFarmIncome() - farm.getIncome());
            updateTotalIncome(player);
        }
    }

    public void removeUnitUpkeep(Player player, int upkeep) {
        player.setUnitUpkeep(player.getUnitUpkeep() - upkeep);
        updateTotalIncome(player);
    }

}
