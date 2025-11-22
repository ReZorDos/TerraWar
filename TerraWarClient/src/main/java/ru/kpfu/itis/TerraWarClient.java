package ru.kpfu.itis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.service.GameMapService;
import ru.kpfu.itis.view.GameMapPane;

public class TerraWarClient extends Application {

    private GameMap gameMap;
    private GameMapService gameMapService;

    @Override
    public void start(Stage stage) throws Exception {
        initializeTestMap();
        GameMapPane gameMapPane = new GameMapPane(gameMap, gameMapService);

        Scene scene = new Scene(gameMapPane, 900, 600);
        stage.setScene(scene);

        stage.show();
    }

    private void initializeTestMap() {
        gameMap = new GameMap(10, 10);
        gameMapService = new GameMapService(gameMap);

        gameMap.getHex(1, 1).setOwnerId(0);
        gameMap.getHex(1, 1).setCapital(true);
        gameMap.getHex(1, 1).setUnitLevel(1);

        gameMap.getHex(2, 1).setOwnerId(0);
        gameMap.getHex(2, 1).setUnitLevel(2);

        gameMap.getHex(4, 4).setOwnerId(1);
        gameMap.getHex(4, 4).setCapital(true);
        gameMap.getHex(4, 4).setUnitLevel(1);

        gameMap.getHex(3, 4).setOwnerId(1);
        gameMap.getHex(3, 4).setUnitLevel(1);

        gameMap.getHex(2, 2).setType(ru.kpfu.itis.enums.Type.FOREST);
        gameMap.getHex(3, 3).setType(ru.kpfu.itis.enums.Type.MOUNTAIN);
        gameMap.getHex(4, 2).setType(ru.kpfu.itis.enums.Type.DESERT);
    }

    public static void main(String[] args) {
        launch(args);
    }

}