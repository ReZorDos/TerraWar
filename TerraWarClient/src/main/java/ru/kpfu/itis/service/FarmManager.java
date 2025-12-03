package ru.kpfu.itis.service;

import lombok.Getter;
import ru.kpfu.itis.model.Farm;
import ru.kpfu.itis.model.Player;
import java.util.*;

@Getter
public class FarmManager {

    private final Map<Integer, List<Farm>> playerFarms;
    private int farmIdCounter = 0;
    private final Game game;
    private final PlayerService playerService;
    private final static int BASE_COST_FARM = 12;

    public FarmManager(Game game, PlayerService playerService) {
        this.playerFarms = new HashMap<>();
        this.game = game;
        this.playerService = playerService;
    }

    public Farm createFarm(int ownerId, int hexX, int hexY) {
        Farm farm = new Farm(farmIdCounter++, ownerId, hexX, hexY);
        playerFarms.putIfAbsent(ownerId, new ArrayList<>());
        playerFarms.get(ownerId).add(farm);

        Player player = getPlayerById(ownerId);
        if (player != null) {
            playerService.addFarm(player, farm);
        }

        return farm;
    }

    public Farm getFarmAt(int hexX, int hexY) {
        for (List<Farm> farms : playerFarms.values()) {
            for (Farm farm : farms) {
                if (farm.getHexX() == hexX && farm.getHexY() == hexY) {
                    return farm;
                }
            }
        }
        return null;
    }

    public List<Farm> getPlayerFarms(int playerId) {
        return playerFarms.getOrDefault(playerId, new ArrayList<>());
    }

    public void removeFarm(int farmId) {
        for (Map.Entry<Integer, List<Farm>> entry : playerFarms.entrySet()) {
            Iterator<Farm> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                Farm farm = iterator.next();
                if (farm.getId() == farmId) {
                    Player player = getPlayerById(farm.getOwnerId());
                    if (player != null) {
                        playerService.removeFarm(player, farm);
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

    public int getFarmPriceForPlayer(int playerId) {
        Player player = getPlayerById(playerId);
        if (player == null) return Integer.MAX_VALUE;

        return BASE_COST_FARM + (player.getFarms().size() * 2);
    }
}
