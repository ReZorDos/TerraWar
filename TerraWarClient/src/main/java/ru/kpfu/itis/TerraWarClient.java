package ru.kpfu.itis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Player;
import ru.kpfu.itis.service.Game;
import ru.kpfu.itis.service.GameMapService;
import ru.kpfu.itis.view.GameMapPane;

public class TerraWarClient extends Application {

    private GameMap gameMap;
    private GameMapService gameMapService;
    private Game game;

    @Override
    public void start(Stage stage) throws Exception {
        initializeGame();
        GameMapPane gameMapPane = new GameMapPane(gameMap, gameMapService, game);

        Scene scene = new Scene(gameMapPane, 900, 650);
        stage.setScene(scene);
        stage.show();
    }

    private void initializeGame() {
        game = new Game();

        Player player1 = new Player(0, "Red Player", "RED");
        Player player2 = new Player(1, "Blue Player", "BLUE");

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.startGame();

        gameMap = new GameMap(10, 10);
        gameMapService = new GameMapService(gameMap);

        initializeStartingPositions();
    }

    private void initializeStartingPositions() {
        gameMap.getHex(1, 1).setOwnerId(0);
        gameMap.getHex(2, 1).setOwnerId(0);

        gameMap.getHex(4, 4).setOwnerId(1);
        gameMap.getHex(3, 4).setOwnerId(1);

        gameMap.getHex(2, 2).setType(ru.kpfu.itis.enums.Type.FOREST);
        gameMap.getHex(3, 3).setType(ru.kpfu.itis.enums.Type.MOUNTAIN);
        gameMap.getHex(4, 2).setType(ru.kpfu.itis.enums.Type.DESERT);

        gameMap.getHex(1, 2).setType(ru.kpfu.itis.enums.Type.GRASS);
        gameMap.getHex(2, 3).setType(ru.kpfu.itis.enums.Type.GRASS);
        gameMap.getHex(3, 2).setType(ru.kpfu.itis.enums.Type.GRASS);
    }

    public static void main(String[] args) {
        launch(args);
    }

}