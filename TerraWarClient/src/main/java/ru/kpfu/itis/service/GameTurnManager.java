package ru.kpfu.itis.service;

import lombok.Getter;
import lombok.Setter;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.model.Unit;
import java.util.List;

@Getter
@Setter
public class GameTurnManager {

    private Game game;
    private UnitManager unitManager;
    private UnitShop unitShop;
    private TowerManager towerManager;
    private boolean isPlayerTurnActive;

    public GameTurnManager(Game game, UnitManager unitManager, UnitShop unitShop, TowerManager towerManager) {
        this.game = game;
        this.unitManager = unitManager;
        this.unitShop = unitShop;
        this.towerManager = towerManager;
        this.isPlayerTurnActive = true;
    }

    public void startPlayerTurn() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            resetPlayerUnitActions(currentPlayer.getId());
            isPlayerTurnActive = true;
        }
    }

    public void endPlayerTurn() {
        if (!isPlayerTurnActive) return;

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            currentPlayer.setMoney(currentPlayer.getMoney() + currentPlayer.getIncome());
            checkAndHandleBankrot(currentPlayer.getId());
        }

        game.nextTurn();
        isPlayerTurnActive = false;
    }

    public void updatePlayerMoneyForTurnEnd(int playerId) {
        Player player = game.getPlayers().stream()
                .filter(p -> p.getId() == playerId)
                .findFirst()
                .orElse(null);
        if (player != null) {
            player.setMoney(player.getMoney() + player.getIncome());
            checkAndHandleBankrot(playerId);
        }
    }

    private void resetPlayerUnitActions(int playerId) {
        List<Unit> playerUnits = unitManager.getPlayerUnits(playerId);
        for (Unit unit : playerUnits) {
            unit.resetTurnActions();
        }
    }

    public void checkAndHandleBankrot(int playerId) {
        Player player = unitManager.getPlayerById(playerId);
        
        if (player.getMoney() <= 0) {
            unitManager.removeAllPlayerUnits(playerId);
            player.setMoney(0);
        }
    }
}