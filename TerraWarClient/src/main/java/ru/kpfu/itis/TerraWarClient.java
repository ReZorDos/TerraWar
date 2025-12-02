package ru.kpfu.itis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.service.*;
import ru.kpfu.itis.view.GameMapPane;

public class TerraWarClient extends Application {

    private GameMap gameMap;
    private GameMapService gameMapService;
    private GameActionService gameActionService;
    private Game game;
    private UnitManager unitManager;
    private UnitShop unitShop;
    private FarmManager farmManager;
    private FarmShop farmShop;
    private GameTurnManager turnManager;
    private PlayerService playerService;

    @Override
    public void start(Stage stage) throws Exception {
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));

        initializeGame();

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
                farmShop
        );

        Scene scene = new Scene(gameMapPane, 900, 650);
        stage.setScene(scene);
        stage.setTitle("TerraWar");
        stage.show();
    }

    private void initializeGame() {
        game = new Game();
        Player player1 = new Player(0, "Красный игрок", "RED");
        Player player2 = new Player(1, "Синий игрок", "BLUE");

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.startGame();

        PlayerService playerService = new PlayerService();

        unitManager = new UnitManager(game, playerService);
        unitShop = new UnitShop();
        farmManager = new FarmManager(game, playerService);
        farmShop = new FarmShop(farmManager);
        turnManager = new GameTurnManager(game, unitManager, unitShop);

        gameMap = new GameMap(10, 10);
        gameMapService = new GameMapService(gameMap);

        gameActionService = new GameActionService(
                gameMap,
                gameMapService,
                unitManager,
                farmManager,
                game
        );

        initializeStartingPositions();
        initializeStartingUnits();
    }

    private void initializeStartingPositions() {
        gameMap.getHex(1, 1).setOwnerId(0);
        gameMap.getHex(2, 1).setOwnerId(0);
        gameMap.getHex(1, 2).setOwnerId(0);

        gameMap.getHex(8, 8).setOwnerId(1);
        gameMap.getHex(8, 9).setOwnerId(1);
        gameMap.getHex(7, 8).setOwnerId(1);
    }

    private void initializeStartingUnits() {

    }

    public static void main(String[] args) {
        launch(args);
    }
}