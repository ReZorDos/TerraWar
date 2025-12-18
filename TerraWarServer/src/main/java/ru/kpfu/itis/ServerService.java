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
        }
        
        broadcastGameState();
        log.info("Удален игрок: {}", leavingPlayerNick);
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
        GameStateMessage gameStateMessage = new GameStateMessage(
                gameState.getPlayers(),
                gameState.getCurrentTurn(),
                lastStateSnapshot,
                gameState.getReadyPlayers(),
                lastStateSnapshot != null
        );
        broadcast(new MessageEnvelope("state", gameStateMessage));
    }

    public boolean applyClientState(PlayerHandler handler, FullGameState snapshot) {
        if (snapshot == null) {
            log.warn("Состояние не обновлено: snapshot null от {}", handler.getNick());
            return false;
        }

        if (lastStateSnapshot == null) {
            boolean allReady = gameState.areAllPlayersReady();
            System.out.println("First state update from " + handler.getNick() + ". All ready: " + allReady);
            if (!allReady) {
                System.out.println("State update rejected: not all players ready. Ready status: " + gameState.getReadyPlayers());
                return false;
            }
            System.out.println("All players ready! Starting game with state from " + handler.getNick());
        } else {
            if (!gameState.isPlayersTurn(handler.getNick())) {
                System.out.println("State update rejected: not " + handler.getNick() + " turn. Current: " + gameState.getCurrentPlayerNick());
                return false;
            }
        }
        lastStateSnapshot = snapshot;
        System.out.println("State update accepted from " + handler.getNick());
        return true;
    }

    public void handleLeave(PlayerHandler handler) {
        log.info("Игрок сдался: {}", handler.getNick());
        removePlayer(handler);
    }
    
    public void handleReady(PlayerHandler handler, ReadyMessage msg) {
        String nick = handler.getNick();
        boolean ready = msg != null && msg.isReady();
        gameState.setPlayerReady(nick, ready);
        System.out.println("Player " + nick + " ready status: " + ready);
        System.out.println("Current ready status: " + gameState.getReadyPlayers());
        boolean allReady = gameState.areAllPlayersReady();
        System.out.println("All players ready: " + allReady);
        broadcastGameState();
    }

    public void broadcast(MessageEnvelope envelope) {
        for (PlayerHandler p : players) {
            p.sendEnvelope(envelope);
        }
    }

}