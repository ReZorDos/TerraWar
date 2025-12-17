package ru.kpfu.itis.network.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
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
            System.err.println("Failed to connect to server: " + e.getMessage());
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

    private void sendMessage(String type, Object data) {
        try {
            MessageEnvelope envelope = new MessageEnvelope(type, data);
            String json = gson.toJson(envelope);
            writer.println(json);
            writer.flush();
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
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
                    System.err.println("Error parsing message from server: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (connected.get()) {
                System.err.println("Connection lost: " + e.getMessage());
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
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected.get();
    }
}

