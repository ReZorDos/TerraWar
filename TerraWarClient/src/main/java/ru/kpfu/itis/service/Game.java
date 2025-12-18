package ru.kpfu.itis.service;

import lombok.Getter;
import lombok.Setter;
import ru.kpfu.itis.model.Player;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Game {

    private final List<Player> players;
    private int currentPlayerIndex;
    private boolean gameStarted;

    public Game() {
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.gameStarted = false;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void startGame() {
        if (players.size() >= 2) {
            gameStarted = true;
            currentPlayerIndex = 0;
        }
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public void nextTurn() {
        if (players.isEmpty()) return;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

}