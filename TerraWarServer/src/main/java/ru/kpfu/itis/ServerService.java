package ru.kpfu.itis;

import ru.kpfu.itis.dto.FullGameState;
import ru.kpfu.itis.message.ConnectPlayerMessage;
import ru.kpfu.itis.message.GameStateMessage;
import ru.kpfu.itis.message.LeaveMessage;
import ru.kpfu.itis.message.MessageEnvelope;
import ru.kpfu.itis.message.ReadyMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerService {

    private final ExecutorService acceptorPool = Executors.newSingleThreadExecutor();
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final CopyOnWriteArrayList<PlayerHandler> players = new CopyOnWriteArrayList<>();
    private final GameState gameState = new GameState();
    private volatile FullGameState lastStateSnapshot;
    private ServerSocket serverSocket;

    public void start() throws IOException {
        serverSocket = new ServerSocket(5555);
        log.info("Сервер запустился, порт {}", 5555);
        acceptorPool.submit(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    PlayerHandler handler = new PlayerHandler(socket, this);
                    players.add(handler);
                    clientPool.submit(handler);
                }
            } catch (IOException e) {
                log.error("Получение остановлено: {}", e.getMessage(), e);
            }
        });
    }

    public void stop() throws IOException {
        serverSocket.close();
        acceptorPool.shutdownNow();
        clientPool.shutdownNow();
    }

    public void registerPlayer(ConnectPlayerMessage connectPlayerMessage) {
        gameState.addPlayer(connectPlayerMessage.getNickName());
        log.info("Зарегестрирован новый игрок: {}", connectPlayerMessage.getNickName());
    }

    public void removePlayer(PlayerHandler handler) {
        String leavingPlayerNick = handler.getNick();
        players.remove(handler);
        gameState.removePlayer(leavingPlayerNick);
        
        if (lastStateSnapshot != null) {
            cleanPlayerDataFromState(leavingPlayerNick);
            if (gameState.getPlayers().size() < 2) {
                log.info("Game stopped: not enough players. Remaining: {}", gameState.getPlayers().size());
                lastStateSnapshot = null;
            }
        }
        
        broadcastGameState();
        log.info("Removed player: {}", leavingPlayerNick);
    }

    private void cleanPlayerDataFromState(String playerNick) {
        if (lastStateSnapshot == null || lastStateSnapshot.getPlayersState() == null) return;

        lastStateSnapshot.getPlayersState().stream()
                .filter(ps -> playerNick.equals(ps.getName()))
                .findFirst()
                .ifPresent(playerState -> {
                    int playerId = playerState.getId();

                    Optional.ofNullable(lastStateSnapshot.getHexes())
                            .ifPresent(hexes -> hexes.stream()
                                    .filter(hex -> hex.getOwnerId() == playerId)
                                    .forEach(hex -> hex.setOwnerId(-1)));

                    Optional.ofNullable(lastStateSnapshot.getUnits())
                            .ifPresent(units -> units.removeIf(unit -> unit.getOwnerId() == playerId));

                    Optional.ofNullable(lastStateSnapshot.getTowers())
                            .ifPresent(towers -> towers.removeIf(tower -> tower.getOwnerId() == playerId));

                    Optional.ofNullable(lastStateSnapshot.getFarms())
                            .ifPresent(farms -> farms.removeIf(farm -> farm.getOwnerId() == playerId));

                    lastStateSnapshot.getPlayersState().removeIf(ps -> ps.getId() == playerId);
                });
    }

    public void endTurn() {
        gameState.nextTurn();
    }

    public void broadcastGameState() {
        boolean gameStarted = lastStateSnapshot != null && gameState.getPlayers().size() >= 2 && gameState.areAllPlayersReady();
        GameStateMessage gameStateMessage = new GameStateMessage(
                gameState.getPlayers(),
                gameState.getCurrentTurn(),
                lastStateSnapshot,
                gameState.getReadyPlayers(),
                gameStarted
        );
        broadcast(new MessageEnvelope("state", gameStateMessage));
    }

    public boolean applyClientState(PlayerHandler handler, FullGameState snapshot) {
        if (snapshot == null) {
            log.warn("Состояние не обновлено: snapshot null от {}", handler.getNick());
            return false;
        }

        if (lastStateSnapshot == null) {
            if (gameState.getPlayers().size() < 2) {
                log.warn("State update rejected: not enough players. Current: {}", gameState.getPlayers().size());
                return false;
            }
            boolean allReady = gameState.areAllPlayersReady();
            log.info("First state update from {}. All ready: {}", handler.getNick(), allReady);
            if (!allReady) {
                log.warn("State update rejected: not all players ready. Ready status: {}", gameState.getReadyPlayers());
                return false;
            }
            log.info("All players ready! Starting game with state from {}", handler.getNick());
        } else {
            if (gameState.getPlayers().size() < 2) {
                log.warn("State update rejected: not enough players. Current: {}", gameState.getPlayers().size());
                return false;
            }
            if (!gameState.isPlayersTurn(handler.getNick())) {
                log.warn("State update rejected: not {} turn. Current: {}", handler.getNick(), gameState.getCurrentPlayerNick());
                return false;
            }
        }
        lastStateSnapshot = snapshot;
        log.info("State update accepted from {}", handler.getNick());
        return true;
    }

    public void handleLeave(PlayerHandler handler) {
        log.info("Player left: {}", handler.getNick());
        String leavingNick = handler.getNick();
        boolean wasCurrentTurn = gameState.isPlayersTurn(leavingNick);
        removePlayer(handler);
        
        if (wasCurrentTurn && gameState.getPlayers().size() > 0) {
            gameState.nextTurn();
            log.info("Turn advanced to next player after {} left", leavingNick);
        }
    }
    
    public void handleReady(PlayerHandler handler, ReadyMessage msg) {
        String nick = handler.getNick();
        boolean ready = msg != null && msg.isReady();
        gameState.setPlayerReady(nick, ready);
        log.info("Player {} ready status: {}", nick, ready);
        log.debug("Current ready status: {}", gameState.getReadyPlayers());
        boolean allReady = gameState.areAllPlayersReady();
        log.info("All players ready: {}", allReady);
        broadcastGameState();
    }

    public void broadcast(MessageEnvelope envelope) {
        for (PlayerHandler p : players) {
            p.sendEnvelope(envelope);
        }
    }

}