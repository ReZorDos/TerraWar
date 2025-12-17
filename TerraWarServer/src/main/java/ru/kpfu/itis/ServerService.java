package ru.kpfu.itis;

import ru.kpfu.itis.dto.FullGameState;
import ru.kpfu.itis.message.ConnectPlayerMessage;
import ru.kpfu.itis.message.GameStateMessage;
import ru.kpfu.itis.message.LeaveMessage;
import ru.kpfu.itis.message.MessageEnvelope;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.*;

public class ServerService {

    private final ExecutorService acceptorPool = Executors.newSingleThreadExecutor();
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final CopyOnWriteArrayList<PlayerHandler> players = new CopyOnWriteArrayList<>();
    private final GameState gameState = new GameState();
    private volatile FullGameState lastStateSnapshot;
    private ServerSocket serverSocket;

    public void start() throws IOException {
        serverSocket = new ServerSocket(5555);
        System.out.println("Server started on port " + 5555);
        acceptorPool.submit(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Accepted: " + socket.getInetAddress());
                    PlayerHandler handler = new PlayerHandler(socket, this);
                    players.add(handler);
                    clientPool.submit(handler);
                }
            } catch (IOException e) {
                System.err.println("Acceptor stopped: " + e.getMessage());
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
        System.out.println("Registered player: " + connectPlayerMessage.getNickName());
    }

    public void removePlayer(PlayerHandler handler) {
        String leavingPlayerNick = handler.getNick();
        players.remove(handler);
        gameState.removePlayer(leavingPlayerNick);
        
        if (lastStateSnapshot != null) {
            cleanPlayerDataFromState(leavingPlayerNick);
        }
        
        broadcastGameState();
        System.out.println("Removed player: " + leavingPlayerNick);
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
                lastStateSnapshot
        );
        broadcast(new MessageEnvelope("state", gameStateMessage));
    }

    public boolean applyClientState(PlayerHandler handler, FullGameState snapshot) {
        if (snapshot == null) {
            System.out.println("State update rejected: null snapshot");
            return false;
        }

        if (!gameState.isPlayersTurn(handler.getNick())) {
            System.out.println("State update rejected: not " + handler.getNick() + " turn. Current: " + gameState.getCurrentPlayerNick());
            return false;
        }

        lastStateSnapshot = snapshot;
        return true;
    }

    public void handleLeave(PlayerHandler handler, LeaveMessage msg) {
        String reason = (msg != null) ? msg.getReason() : null;
        System.out.println("Player leaving: " + handler.getNick() + (reason != null ? " (" + reason + ")" : ""));
        removePlayer(handler);
    }

    public void broadcast(MessageEnvelope envelope) {
        for (PlayerHandler p : players) {
            p.sendEnvelope(envelope);
        }
    }

}