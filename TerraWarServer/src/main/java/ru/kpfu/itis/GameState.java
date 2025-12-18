package ru.kpfu.itis;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    private final List<String> players = new ArrayList<>();
    private int currentTurn = 0;

    public synchronized void addPlayer(String nick) {
        players.add(nick);
    }

    public synchronized void removePlayer(String nick) {
        if (players.isEmpty()) return;

        int removedIndex = players.indexOf(nick);
        boolean wasCurrent = removedIndex == currentTurn;

        if (removedIndex >= 0) {
            players.remove(removedIndex);
        }

        if (players.isEmpty()) {
            currentTurn = 0;
            return;
        }

        if (wasCurrent) {
            currentTurn = removedIndex % players.size();
        } else if (removedIndex >= 0 && removedIndex < currentTurn) {
            currentTurn = Math.max(0, currentTurn - 1);
        }
    }

    public synchronized List<String> getPlayers() {
        return new ArrayList<>(players);
    }

    public synchronized int getCurrentTurn() {
        return currentTurn;
    }

    public synchronized void nextTurn() {
        if (players.isEmpty()) return;
        currentTurn = (currentTurn + 1) % players.size();
    }

    public synchronized boolean isPlayersTurn(String nick) {
        if (players.isEmpty()) return false;
        String current = players.get(currentTurn);
        return current != null && current.equals(nick);
    }

}