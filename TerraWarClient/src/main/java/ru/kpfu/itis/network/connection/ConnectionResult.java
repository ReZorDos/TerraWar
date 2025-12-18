package ru.kpfu.itis.network.connection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ConnectionResult {

    private final String serverHost;
    private final int serverPort;
    private final String playerName;
    private final int playerIndex;

}