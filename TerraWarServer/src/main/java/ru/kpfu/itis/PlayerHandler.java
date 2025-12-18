package ru.kpfu.itis;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.kpfu.itis.dto.FullGameState;
import ru.kpfu.itis.message.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlayerHandler implements Runnable {

    private final Socket socket;
    private final ServerService server;
    private final Gson gson = new Gson();
    private PrintWriter writer;
    private BufferedReader reader;
    private String nick = "unknown";
    private final AtomicBoolean running = new AtomicBoolean(true);

    public PlayerHandler(Socket socket, ServerService server) {
        this.socket = socket;
        this.server = server;
    }

    public String getNick() {
        return nick;
    }

    public void sendEnvelope(MessageEnvelope envelope) {
        try {
            String json = gson.toJson(envelope);
            writer.println(json);
            writer.flush();
        } catch (Exception e) {
            shutdown();
        }
    }

    private void shutdown() {
        running.set(false);
        try { reader.close(); } catch (Exception ignored) {}
        try { writer.close(); } catch (Exception ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
        server.removePlayer(this);
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                log.debug("Сервер читает из {}: {}", socket.getInetAddress(), line);
                try {
                    JsonElement jsonElement = JsonParser.parseString(line);
                    if (!jsonElement.isJsonObject()) continue;
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : null;
                    JsonElement dataEl = jsonObject.get("data");

                    if ("connect".equals(type)) {
                        ConnectPlayerMessage connectPlayerMsg = gson.fromJson(dataEl, ConnectPlayerMessage.class);
                        this.nick = connectPlayerMsg.getNickName();
                        server.registerPlayer(connectPlayerMsg);
                        MessageResponse<String> resp = new MessageResponse<>(true, "connected", nick);
                        sendEnvelope(new MessageEnvelope("response", resp));
                        server.broadcastGameState();
                    } else if ("stateUpdate".equals(type)) {
                        FullGameState fullGameState = gson.fromJson(dataEl, FullGameState.class);
                        boolean ok = server.applyClientState(this, fullGameState);
                        MessageResponse<String> resp = new MessageResponse<>(ok, ok ? "state accepted" : "invalid state or not your turn", null);
                        sendEnvelope(new MessageEnvelope("response", resp));
                        if (ok) {
                            server.broadcastGameState();
                        }
                    } else if ("endTurn".equals(type)) {
                        server.endTurn();
                        MessageResponse<String> resp = new MessageResponse<>(true, "turn ended", null);
                        sendEnvelope(new MessageEnvelope("response", resp));
                        server.broadcastGameState();
                    } else if ("leave".equals(type)) {
                        LeaveMessage lm = gson.fromJson(dataEl, LeaveMessage.class);
                        server.handleLeave(this);
                        MessageResponse<String> resp = new MessageResponse<>(true, "left", null);
                        sendEnvelope(new MessageEnvelope("response", resp));
                    } else if ("ready".equals(type)) {
                        ReadyMessage readyMsg = gson.fromJson(dataEl, ReadyMessage.class);
                        server.handleReady(this, readyMsg);
                        MessageResponse<String> resp = new MessageResponse<>(true, "ready status updated", null);
                        sendEnvelope(new MessageEnvelope("response", resp));
                    } else {
                        MessageResponse<String> resp = new MessageResponse<>(false, "unknown type", null);
                        sendEnvelope(new MessageEnvelope("response", resp));
                    }
                } catch (Exception e) {
                    log.error("Error handling message: {}", e.getMessage(), e);
                    MessageResponse<String> resp = new MessageResponse<>(false, "server error: " + e.getMessage(), null);
                    sendEnvelope(new MessageEnvelope("response", resp));
                }
            }
        } catch (IOException e) {
            log.error("Проблемы у {}: {}", nick, e.getMessage(), e);
        } finally {
            shutdown();
        }
    }
}