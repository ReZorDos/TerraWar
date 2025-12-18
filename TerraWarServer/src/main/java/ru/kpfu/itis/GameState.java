package ru.kpfu.itis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {

    private final List<String> players = new ArrayList<>();
    private final Map<String, Boolean> readyPlayers = new HashMap<>();
    private int currentTurn = 0;

    public synchronized void addPlayer(String nick) {
        players.add(nick);
        readyPlayers.put(nick, false);
    }

    public synchronized void removePlayer(String nick) {
        if (players.isEmpty()) return;

        int removedIndex = players.indexOf(nick);
        boolean wasCurrent = removedIndex == currentTurn;

        if (removedIndex >= 0) {
            players.remove(removedIndex);
            readyPlayers.remove(nick);
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
    
    public synchronized void setPlayerReady(String nick, boolean ready) {
        readyPlayers.put(nick, ready);
    }
    
    public synchronized Map<String, Boolean> getReadyPlayers() {
        return new HashMap<>(readyPlayers);
    }
    
    public synchronized boolean areAllPlayersReady() {
        if (players.size() < 2) return false;
        for (String player : players) {
            if (!readyPlayers.getOrDefault(player, false)) {
                return false;
            }
        }
        return true;
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

    public synchronized String getCurrentPlayerNick() {
        if (players.isEmpty()) return null;
        return players.get(currentTurn);
    }

}