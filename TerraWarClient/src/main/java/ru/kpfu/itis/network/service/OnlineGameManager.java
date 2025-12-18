package ru.kpfu.itis.network.service;

import javafx.application.Platform;
import ru.kpfu.itis.dto.FullGameState;
import ru.kpfu.itis.message.GameStateMessage;
import ru.kpfu.itis.message.MessageResponse;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OnlineGameManager {

    private NetworkClient networkClient;
    private final GameMap gameMap;
    private final UnitManager unitManager;
    private final TowerManager towerManager;
    private final FarmManager farmManager;
    private final Game game;
    private final PlayerService playerService;
    private final GameTurnManager turnManager;
    private String myNickName;
    private int myPlayerIndex;
    private boolean isMyTurn = false;
    private int lastServerTurn = -1;
    private List<String> serverPlayers;
    private Consumer<String> onErrorCallback;
    private Runnable onStateUpdatedCallback;
    
    public OnlineGameManager(
            String serverHost,
            int serverPort,
            GameMap gameMap,
            UnitManager unitManager,
            TowerManager towerManager,
            FarmManager farmManager,
            Game game,
            PlayerService playerService,
            GameTurnManager turnManager) {
        
        this.gameMap = gameMap;
        this.unitManager = unitManager;
        this.towerManager = towerManager;
        this.farmManager = farmManager;
        this.game = game;
        this.playerService = playerService;
        this.turnManager = turnManager;
        
        this.networkClient = new NetworkClient(serverHost, serverPort);
        setupNetworkCallbacks();
    }

    public void setNetworkClient(NetworkClient client, String myNickName, int myPlayerIndex) {
        this.networkClient = client;
        this.myNickName = myNickName;
        this.myPlayerIndex = myPlayerIndex;
        setupNetworkCallbacks();
    }

    private void setupNetworkCallbacks() {
        networkClient.setOnStateReceived(this::handleStateMessage);
        networkClient.setOnResponseReceived(this::handleResponse);
    }

    public void sendStateUpdate() {
        FullGameState state = StateConverter.toFullGameState(
                gameMap, unitManager, towerManager, farmManager, game);
        networkClient.sendStateUpdate(state);
    }

    public void sendEndTurn() {
        if (!networkClient.isConnected()) {
            return;
        }
        networkClient.sendEndTurn();
    }

    public void sendSurrender() {
        if (!networkClient.isConnected()) {
            return;
        }
        networkClient.sendLeave("surrender");
    }

    private void handleStateMessage(GameStateMessage stateMsg) {
        Platform.runLater(() -> {
            serverPlayers = stateMsg.getPlayers();
            
            if (stateMsg.getStateSnapshot() != null) {
                StateConverter.applyFullGameState(
                        stateMsg.getStateSnapshot(),
                        gameMap, unitManager, towerManager, farmManager,
                        game, playerService, serverPlayers);
            }
            
            int serverCurrentTurn = stateMsg.getCurrentTurn();
            
            if (serverPlayers != null && !serverPlayers.isEmpty() &&
                serverCurrentTurn >= 0 && serverCurrentTurn < serverPlayers.size()) {
                syncLocalGameState(serverPlayers, serverCurrentTurn);
                
                int myIndexOnServer = findMyPlayerIndex(serverPlayers, myNickName);
                if (myIndexOnServer >= 0) {
                    myPlayerIndex = myIndexOnServer;
                    
                    isMyTurn = (serverCurrentTurn == myIndexOnServer);
                    if (isMyTurn && serverCurrentTurn != lastServerTurn) {
                        turnManager.startPlayerTurn();
                    } else {
                        turnManager.setPlayerTurnActive(false);
                    }
                    lastServerTurn = serverCurrentTurn;
                } else {
                    isMyTurn = false;
                    turnManager.setPlayerTurnActive(false);
                }
            }
            if (onStateUpdatedCallback != null) {
                onStateUpdatedCallback.run();
            }
        });
    }

    private int findMyPlayerIndex(List<String> serverPlayers, String myNickName) {
        if (myPlayerIndex >= 0 && myPlayerIndex < serverPlayers.size()) {
            String nameAtPosition = serverPlayers.get(myPlayerIndex);
            if (nameAtPosition != null && nameAtPosition.equals(myNickName)) {
                return myPlayerIndex;
            }
        }
        Player myPlayer = getMyPlayer();
        if (myPlayer != null && myPlayer.getId() >= 0 && myPlayer.getId() < serverPlayers.size()) {
            return myPlayer.getId();
        }
        
        return serverPlayers.indexOf(myNickName);
    }

    private void syncLocalGameState(List<String> serverPlayers, int serverCurrentTurn) {
        if (serverPlayers == null || serverPlayers.isEmpty()) {
            return;
        }
        List<Player> localPlayers = game.getPlayers();
        localPlayers.removeIf(player -> !serverPlayers.contains(player.getName()));

        List<Player> orderedPlayers = new ArrayList<>();
        for (String serverPlayerName : serverPlayers) {
            Player localPlayer = localPlayers.stream()
                    .filter(p -> p.getName().equals(serverPlayerName))
                    .findFirst()
                    .orElse(null);
            if (localPlayer != null) {
                orderedPlayers.add(localPlayer);
            }
        }
        localPlayers.clear();
        localPlayers.addAll(orderedPlayers);
        if (serverCurrentTurn >= 0 && serverCurrentTurn < serverPlayers.size()) {
            String currentPlayerName = serverPlayers.get(serverCurrentTurn);
            int localIndex = -1;
            for (int i = 0; i < game.getPlayers().size(); i++) {
                if (game.getPlayers().get(i).getName().equals(currentPlayerName)) {
                    localIndex = i;
                    break;
                }
            }
            if (localIndex >= 0) {
                game.setCurrentPlayerIndex(localIndex);
            }
        }
    }

    private void handleResponse(MessageResponse<String> response) {
        Platform.runLater(() -> {
            if (!response.isSuccess() && onErrorCallback != null) {
                onErrorCallback.accept(response.getMessage());
            }
        });
    }

    public void setOnError(Consumer<String> callback) {
        this.onErrorCallback = callback;
    }

    public void setOnStateUpdated(Runnable callback) {
        this.onStateUpdatedCallback = callback;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public boolean isConnected() {
        return networkClient.isConnected();
    }

    public Player getMyPlayer() {
        if (myNickName == null || game == null) {
            return game != null ? game.getCurrentPlayer() : null;
        }
        return game.getPlayers().stream()
                .filter(p -> myNickName.equals(p.getName()))
                .findFirst()
                .orElse(game.getCurrentPlayer());
    }
    
    public List<String> getServerPlayers() {
        return serverPlayers;
    }
}

