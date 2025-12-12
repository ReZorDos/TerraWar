package ru.kpfu.itis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.kpfu.itis.enums.MapType;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.service.*;
import ru.kpfu.itis.service.MapFactory;
import ru.kpfu.itis.view.GameMapPane;
import java.util.Random;

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
                farmShop,
                towerManager,
                towerShop
        );

        Scene scene = new Scene(gameMapPane, 900, 700);
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

        playerService = new PlayerService();
        unitManager = new UnitManager(game, playerService);
        unitShop = new UnitShop();
        farmManager = new FarmManager(game, playerService);
        farmShop = new FarmShop(farmManager);

        MapType[] mapTypes = MapType.values();
        MapType randomMapType = mapTypes[new Random().nextInt(mapTypes.length)];
        gameMap = MapFactory.getRandomMap();
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

        initializeStartingUnits();
    }

    private void initializeStartingUnits() {

    }

    public static void main(String[] args) {
        launch(args);
    }
}