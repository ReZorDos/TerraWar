package ru.kpfu.itis;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.network.service.NetworkClient;
import ru.kpfu.itis.network.service.OnlineGameManager;
import ru.kpfu.itis.service.*;
import ru.kpfu.itis.service.MapFactory;
import ru.kpfu.itis.view.ConnectionDialog;
import ru.kpfu.itis.network.connection.ConnectionResult;
import ru.kpfu.itis.view.GameMapPane;
import ru.kpfu.itis.view.WaitingScreen;

import java.util.List;
import java.util.Map;

@Slf4j
public class TerraWarClient extends Application {

    private GameMap gameMap;
    private GameMapService gameMapService;
    private GameActionService gameActionService;
    private Game game;
    private UnitManager unitManager;
    private UnitShop unitShop;
    private FarmManager farmManager;
    private FarmShop farmShop;
    private TowerManager towerManager;
    private TowerShop towerShop;
    private GameTurnManager turnManager;
    private PlayerService playerService;
    private OnlineGameManager onlineGameManager;
    private Stage mainStage;

    @Override
    public void start(Stage stage) throws Exception {
        this.mainStage = stage;

        log.info("Showing connection dialog...");
        ConnectionResult connectionResult = ConnectionDialog.showAndWait();

        if (connectionResult == null) {
            log.info("User cancelled connection");
            System.exit(0);
            return;
        }

        log.info("Connecting to server: {}:{}", connectionResult.getServerHost(), connectionResult.getServerPort());

        WaitingScreen waitingScreen = new WaitingScreen();
        Stage waitingStage = new Stage();
        waitingStage.setTitle("Ожидание игроков...");
        waitingStage.setScene(new Scene(waitingScreen, 400, 300));
        waitingStage.show();

        NetworkClient tempNetworkClient = new NetworkClient(connectionResult.getServerHost(), connectionResult.getServerPort());
        
        log.info("Attempting to connect to server...");
        if (!tempNetworkClient.connect()) {
            waitingStage.close();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка подключения");
            alert.setHeaderText(null);
            alert.setContentText("Не удалось подключиться к серверу " + connectionResult.getServerHost() + ":" + connectionResult.getServerPort() +
                    "\nПроверьте, что сервер запущен и адрес указан правильно.");
            alert.showAndWait();
            System.exit(0);
            return;
        }

        tempNetworkClient.sendConnect(connectionResult.getPlayerName(), -1);

        waitingScreen.setOnReadyCallback(ready -> {
            tempNetworkClient.sendReady(ready);
        });

        final boolean[] gameStarted = {false};
        tempNetworkClient.setOnStateReceived(stateMsg -> {
            if (gameStarted[0]) return;
            
            List<String> players = stateMsg.getPlayers();
            if (players != null) {
                Platform.runLater(() -> {
                    waitingScreen.updatePlayers(players, stateMsg.getReadyPlayers(), connectionResult.getPlayerName());
                });
                
                Map<String, Boolean> readyPlayers = stateMsg.getReadyPlayers();
                boolean allReady = false;
                if (readyPlayers != null && players.size() >= 2) {
                    allReady = true;
                    for (String player : players) {
                        if (!readyPlayers.getOrDefault(player, false)) {
                            allReady = false;
                            break;
                        }
                    }
                }
                
                if ((allReady || stateMsg.isGameStarted()) && !gameStarted[0]) {
                    gameStarted[0] = true;
                    log.info("All players ready! Starting game...");
                    Platform.runLater(() -> {
                        waitingStage.close();
                        initializeGame(connectionResult, players, tempNetworkClient);
                    });
                }
            }
        });

        tempNetworkClient.setOnResponseReceived(response -> {
            if (!response.isSuccess()) {
                Platform.runLater(() -> {
                    waitingStage.close();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка");
                    alert.setHeaderText(null);
                    alert.setContentText(response.getMessage());
                    alert.showAndWait();
                    System.exit(0);
                });
            }
        });
    }

    private void initializeGame(ConnectionResult connectionResult, List<String> serverPlayers, NetworkClient networkClient) {
        game = new Game();

        String[] colors = {"RED", "BLUE", "YELLOW", "PINK"};
        for (int i = 0; i < serverPlayers.size() && i < colors.length; i++) {
            String playerName = serverPlayers.get(i);
            Player player = new Player(i, playerName, colors[i]);
            game.addPlayer(player);
        }

        game.startGame();

        int myIndexOnServer = serverPlayers.indexOf(connectionResult.getPlayerName());
        if (myIndexOnServer < 0) {
            myIndexOnServer = 0;
        }

        playerService = new PlayerService();
        unitManager = new UnitManager(game, playerService);
        unitShop = new UnitShop();
        farmManager = new FarmManager(game, playerService);
        farmShop = new FarmShop(farmManager);

        int mapId = 0;
        if (myIndexOnServer == 0) {
            mapId = (int) (Math.random() * 5);
            log.info("Selected random map with id = {} ({})", mapId, MapFactory.getMapNameById(mapId));
        }

        gameMap = MapFactory.getMapById(mapId);
        gameMapService = new GameMapService(gameMap);

        towerManager = new TowerManager(game, playerService, gameMapService);
        towerShop = new TowerShop(towerManager);

        turnManager = new GameTurnManager(game, unitManager, unitShop, towerManager);

        gameActionService = new GameActionService(
                gameMap,
                gameMapService,
                unitManager,
                farmManager,
                game,
                towerManager
        );

        onlineGameManager = new OnlineGameManager(
                connectionResult.getServerHost(),
                connectionResult.getServerPort(),
                gameMap,
                unitManager,
                towerManager,
                farmManager,
                game,
                playerService,
                turnManager
        );

        onlineGameManager.setNetworkClient(networkClient, connectionResult.getPlayerName(), myIndexOnServer);

        if (myIndexOnServer == 0) {
            onlineGameManager.sendStateUpdate();
        }

        Platform.runLater(() -> {
            GameMapPane gameMapPane = new GameMapPane(
                    gameMap,
                    gameActionService,
                    playerService,
                    game,
                    turnManager,
                    unitManager,
                    unitShop,
                    gameMapService,
                    farmManager,
                    farmShop,
                    towerManager,
                    towerShop,
                    onlineGameManager
            );

            Scene scene = new Scene(gameMapPane, 1280, 860);
            scene.setFill(Color.web("#2b2b2b"));
            mainStage.setScene(scene);
            mainStage.setTitle("TerraWar");
            mainStage.setMaximized(true);
            mainStage.show();
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}