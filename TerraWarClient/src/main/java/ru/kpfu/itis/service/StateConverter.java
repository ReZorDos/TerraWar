package ru.kpfu.itis.service;

import ru.kpfu.itis.dto.*;
import ru.kpfu.itis.enums.Type;
import ru.kpfu.itis.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StateConverter {

    public static FullGameState toFullGameState(
            GameMap gameMap,
            UnitManager unitManager,
            TowerManager towerManager,
            FarmManager farmManager,
            Game game) {
        
        FullGameState state = new FullGameState();
        state.setMapWidth(gameMap.getWidth());
        state.setMapHeight(gameMap.getHeight());
        
        List<HexState> hexes = new ArrayList<>();
        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hex hex = gameMap.getHex(x, y);
                if (hex != null) {
                    HexState hexState = new HexState();
                    hexState.setX(hex.getX());
                    hexState.setY(hex.getY());
                    hexState.setType(hex.getType().name());
                    hexState.setOwnerId(hex.getOwnerId());
                    hexState.setCapital(hex.isCapital());
                    
                    Unit unit = unitManager.getUnitAt(x, y);
                    hexState.setUnitLevel(unit != null ? unit.getLevel() : 0);
                    
                    hexes.add(hexState);
                }
            }
        }
        state.setHexes(hexes);
        
        List<UnitState> units = unitManager.getAllUnits().stream()
                .map(unit -> {
                    UnitState us = new UnitState();
                    us.setId(unit.getId());
                    us.setOwnerId(unit.getOwnerId());
                    us.setHexX(unit.getHexX());
                    us.setHexY(unit.getHexY());
                    us.setLevel(unit.getLevel());
                    us.setActionRadius(unit.getActionRadius());
                    us.setHasActed(unit.isHasActed());
                    us.setUpkeepCost(unit.getUpkeepCost());
                    return us;
                })
                .collect(Collectors.toList());
        state.setUnits(units);
        
        List<TowerState> towers = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            List<Tower> playerTowers = towerManager.getPlayerTowers(player.getId());
            for (Tower tower : playerTowers) {
                TowerState ts = new TowerState();
                ts.setId(tower.getId());
                ts.setOwnerId(tower.getOwnerId());
                ts.setHexX(tower.getHexX());
                ts.setHexY(tower.getHexY());
                ts.setLevel(tower.getLevel());
                towers.add(ts);
            }
        }
        state.setTowers(towers);
        
        List<FarmState> farms = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            List<Farm> playerFarms = farmManager.getPlayerFarms(player.getId());
            for (Farm farm : playerFarms) {
                FarmState fs = new FarmState();
                fs.setId(farm.getId());
                fs.setOwnerId(farm.getOwnerId());
                fs.setHexX(farm.getHexX());
                fs.setHexY(farm.getHexY());
                fs.setIncome(farm.getIncome());
                farms.add(fs);
            }
        }
        state.setFarms(farms);
        
        List<PlayerState> playersState = game.getPlayers().stream()
                .map(player -> {
                    PlayerState ps = new PlayerState();
                    ps.setId(player.getId());
                    ps.setName(player.getName());
                    ps.setColor(player.getColor());
                    ps.setMoney(player.getMoney());
                    ps.setIncome(player.getIncome());
                    ps.setBaseIncome(player.getBaseIncome());
                    ps.setUnitUpkeep(player.getUnitUpkeep());
                    ps.setTowerUpkeep(player.getTowerUpkeep());
                    ps.setFarmIncome(player.getFarmIncome());
                    return ps;
                })
                .collect(Collectors.toList());
        state.setPlayersState(playersState);
        
        return state;
    }

    public static void applyFullGameState(
            FullGameState state,
            GameMap gameMap,
            UnitManager unitManager,
            TowerManager towerManager,
            FarmManager farmManager,
            Game game,
            PlayerService playerService,
            List<String> activePlayerNames) {
        
        if (state == null) return;
        
        List<Integer> activePlayerIds = new ArrayList<>();
        if (activePlayerNames != null && state.getPlayersState() != null) {
            for (PlayerState ps : state.getPlayersState()) {
                if (activePlayerNames.contains(ps.getName())) {
                    activePlayerIds.add(ps.getId());
                }
            }
        }
        
        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                Hex hex = gameMap.getHex(x, y);
                if (hex != null) {
                    hex.setUnitLevel(0);
                }
            }
        }

        if (state.getHexes() != null) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                for (int x = 0; x < gameMap.getWidth(); x++) {
                    gameMap.getGrid().get(y).set(x, null);
                }
            }
            
            for (HexState hexState : state.getHexes()) {
                Hex hex = new Hex(hexState.getX(), hexState.getY(), Type.valueOf(hexState.getType()));
                if (hexState.getOwnerId() != -1 && !activePlayerIds.isEmpty() &&
                    !activePlayerIds.contains(hexState.getOwnerId())) {
                    hex.setOwnerId(-1);
                } else {
                    hex.setOwnerId(hexState.getOwnerId());
                }
                hex.setCapital(hexState.isCapital());
                gameMap.getGrid().get(hexState.getY()).set(hexState.getX(), hex);
            }
        }
        
        for (Player player : game.getPlayers()) {
            unitManager.removeAllPlayerUnits(player.getId());
        }
        if (state.getUnits() != null) {
            for (UnitState us : state.getUnits()) {
                if (activePlayerIds.isEmpty() || activePlayerIds.contains(us.getOwnerId())) {
                    Unit unit = unitManager.createUnit(us.getOwnerId(), us.getHexX(), us.getHexY(), us.getLevel());
                    unit.setHasActed(us.isHasActed());
                    Hex hex = gameMap.getHex(us.getHexX(), us.getHexY());
                    if (hex != null) {
                        hex.setUnitLevel(us.getLevel());
                    }
                }
            }
        }
        
        for (Player player : game.getPlayers()) {
            List<Tower> playerTowers = towerManager.getPlayerTowers(player.getId());
            for (Tower tower : new ArrayList<>(playerTowers)) {
                towerManager.removeTower(tower.getId());
            }
        }
        if (state.getTowers() != null) {
            for (TowerState ts : state.getTowers()) {
                if (activePlayerIds.isEmpty() || activePlayerIds.contains(ts.getOwnerId())) {
                    towerManager.createTower(ts.getOwnerId(), ts.getHexX(), ts.getHexY(), ts.getLevel());
                }
            }
        }
        
        for (Player player : game.getPlayers()) {
            List<Farm> playerFarms = farmManager.getPlayerFarms(player.getId());
            for (Farm farm : new ArrayList<>(playerFarms)) {
                farmManager.removeFarm(farm.getId());
            }
        }
        if (state.getFarms() != null) {
            for (FarmState fs : state.getFarms()) {
                if (activePlayerIds.isEmpty() || activePlayerIds.contains(fs.getOwnerId())) {
                    farmManager.createFarm(fs.getOwnerId(), fs.getHexX(), fs.getHexY());
                }
            }
        }
        
        if (state.getPlayersState() != null) {
            for (PlayerState ps : state.getPlayersState()) {
                Player player = game.getPlayers().stream()
                        .filter(p -> p.getId() == ps.getId())
                        .findFirst()
                        .orElse(null);
                if (player != null) {
                    player.setMoney(ps.getMoney());
                    player.setIncome(ps.getIncome());
                    player.setBaseIncome(ps.getBaseIncome());
                    player.setUnitUpkeep(ps.getUnitUpkeep());
                    player.setTowerUpkeep(ps.getTowerUpkeep());
                    player.setFarmIncome(ps.getFarmIncome());
                }
            }
        }
    }
}

