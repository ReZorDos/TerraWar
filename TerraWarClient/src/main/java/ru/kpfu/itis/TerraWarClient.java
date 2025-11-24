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
    private GameTurnManager turnManager;

    @Override
    public void start(Stage stage) throws Exception {
        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));

        initializeGame();

        GameMapPane gameMapPane = new GameMapPane(
                gameMap,
                gameMapService,
                gameActionService,
                game,
                turnManager,
                unitManager
        );

        Scene scene = new Scene(gameMapPane, 900, 650);
        stage.setScene(scene);
        stage.setTitle("TerraWar");
        stage.show();

        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║   🎮 TerraWar - Hex Strategy Game  ║");
        System.out.println("║   Архитектура: Clean Architecture  ║");
        System.out.println("╚════════════════════════════════════╝");
    }

    private void initializeGame() {
        game = new Game();
        Player player1 = new Player(0, "Красный игрок", "RED");
        Player player2 = new Player(1, "Синий игрок", "BLUE");

        player1.setMoney(50);
        player1.setIncome(2);
        player2.setMoney(50);
        player2.setIncome(2);

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.startGame();

        unitManager = new UnitManager();
        turnManager = new GameTurnManager(game, unitManager);

        gameMap = new GameMap(10, 10);
        gameMapService = new GameMapService(gameMap);

        gameActionService = new GameActionService(
                gameMap,
                gameMapService,
                unitManager,
                game
        );

        initializeStartingPositions();
        initializeStartingUnits();

        System.out.println("[INIT] Игра инициализирована");
        System.out.println("[INIT] Игроки: " + player1.getName() + " vs " + player2.getName());
        System.out.println("[INIT] Карта: 10x10 гексов");
    }

    private void initializeStartingPositions() {
        gameMap.getHex(1, 1).setOwnerId(0);
        gameMap.getHex(2, 1).setOwnerId(0);
        gameMap.getHex(1, 2).setOwnerId(0);

        // Синий игрок (правый нижний угол)
        gameMap.getHex(8, 8).setOwnerId(1);
        gameMap.getHex(8, 9).setOwnerId(1);
        gameMap.getHex(7, 8).setOwnerId(1);

        System.out.println("[MAP] Начальные территории захвачены");
    }

    private void initializeStartingUnits() {
        // Красный игрок
        unitManager.createUnit(0, 1, 1, 1);
        unitManager.createUnit(0, 2, 1, 3);
        unitManager.createUnit(0, 1, 2, 2);

        // Синий игрок
        unitManager.createUnit(1, 8, 8, 1);
        unitManager.createUnit(1, 8, 9, 3);
        unitManager.createUnit(1, 7, 8, 2);

        System.out.println("[UNITS] 6 юнитов созданы (3 красных + 3 синих)");
    }

    public static void main(String[] args) {
        launch(args);
    }
}