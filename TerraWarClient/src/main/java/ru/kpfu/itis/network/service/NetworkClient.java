package ru.kpfu.itis.network.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.dto.FullGameState;
import ru.kpfu.itis.message.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
@Setter
@Slf4j
public class NetworkClient {

    private final String serverHost;
    private final int serverPort;
    private final Gson gson = new Gson();
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private Thread readerThread;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Consumer<GameStateMessage> onStateReceived;
    private Consumer<MessageResponse<String>> onResponseReceived;
    
    public NetworkClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected.set(true);
            
            readerThread = new Thread(this::readMessages);
            readerThread.setDaemon(true);
            readerThread.start();
            
            return true;
        } catch (IOException e) {
            log.error("Ошибка подключения к серверу: {}", e.getMessage(), e);
            connected.set(false);
            return false;
        }
    }

    public void sendConnect(String nickName, int playerIndex) {
        ConnectPlayerMessage msg = new ConnectPlayerMessage(nickName, playerIndex);
        sendMessage("connect", msg);
    }

    public void sendStateUpdate(FullGameState state) {
        sendMessage("stateUpdate", state);
    }

    public void sendEndTurn() {
        sendMessage("endTurn", null);
    }
    
    public void sendLeave(String reason) {
        LeaveMessage leaveMsg = new LeaveMessage(reason);
        sendMessage("leave", leaveMsg);
    }
    
    public void sendReady(boolean ready) {
        ReadyMessage readyMsg = new ReadyMessage(ready);
        sendMessage("ready", readyMsg);
    }

    private void sendMessage(String type, Object data) {
        try {
            MessageEnvelope envelope = new MessageEnvelope(type, data);
            String json = gson.toJson(envelope);
            writer.println(json);
            writer.flush();
        } catch (Exception e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage(), e);
            disconnect();
        }
    }

    private void readMessages() {
        try {
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                try {
                    JsonElement jsonElement = JsonParser.parseString(line);
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : null;
                    JsonElement dataEl = jsonObject.get("data");
                    if ("state".equals(type)) {
                        GameStateMessage stateMsg = gson.fromJson(dataEl, GameStateMessage.class);
                        if (onStateReceived != null) {
                            onStateReceived.accept(stateMsg);
                        }
                    } else if ("response".equals(type)) {
                        MessageResponse<String> response = gson.fromJson(dataEl, MessageResponse.class);
                        if (onResponseReceived != null) {
                            onResponseReceived.accept(response);
                        }
                    }
                } catch (Exception e) {
                    log.error("Ошибка получения сообщения от сервера: {}", e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            if (connected.get()) {
                log.error("Соединение потеряно: {}", e.getMessage(), e);
            }
        } finally {
            disconnect();
        }
    }

    public void disconnect() {
        connected.set(false);
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            log.error("Ошибка закрытия подключения: {}", e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        return connected.get();
    }
}

