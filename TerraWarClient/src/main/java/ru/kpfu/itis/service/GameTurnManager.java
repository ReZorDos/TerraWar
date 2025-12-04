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

        checkAndHandleBankruptcy();

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            currentPlayer.setMoney(currentPlayer.getMoney() + currentPlayer.getIncome());
        }

        game.nextTurn();
        isPlayerTurnActive = false;
    }

    private void resetPlayerUnitActions(int playerId) {
        List<Unit> playerUnits = unitManager.getPlayerUnits(playerId);
        for (Unit unit : playerUnits) {
            unit.resetTurnActions();
        }
    }

    public String getTurnInfo() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return "No player";

        return String.format("Игрок: %s | Деньги: %d | Доход: %d",
                currentPlayer.getName(), currentPlayer.getMoney(), currentPlayer.getIncome());
    }

    public void checkAndHandleBankruptcy() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null && currentPlayer.getMoney() <= 0) {
            unitManager.removeAllPlayerUnits(currentPlayer.getId());
        }
    }
}