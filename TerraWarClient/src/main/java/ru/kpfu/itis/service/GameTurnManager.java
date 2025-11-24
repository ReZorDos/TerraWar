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
    private int currentTurn;
    private boolean isPlayerTurnActive;

    public GameTurnManager(Game game, UnitManager unitManager) {
        this.game = game;
        this.unitManager = unitManager;
        this.currentTurn = 1;
        this.isPlayerTurnActive = true;
    }

    public void startPlayerTurn() {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            resetPlayerUnitActions(currentPlayer.getId());
            currentPlayer.setMoney(currentPlayer.getMoney() + currentPlayer.getIncome());
            isPlayerTurnActive = true;
        }
    }

    public void endPlayerTurn() {
        if (!isPlayerTurnActive) return;
        game.nextTurn();
        if (game.getCurrentPlayerIndex() == 0) {
            currentTurn++;
        }
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
        return String.format("Ход %d | Игрок: %s | Деньги: %d | Доход: +%d",
                currentTurn, currentPlayer.getName(), currentPlayer.getMoney(), currentPlayer.getIncome());
    }

    public boolean canCurrentPlayerMove() {
        Player currentPlayer = game.getCurrentPlayer();
        return currentPlayer != null && unitManager.hasUnits(currentPlayer.getId());
    }
}