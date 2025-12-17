package ru.kpfu.itis.view;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class WaitingScreen extends VBox {

    private final Label statusLabel;
    private final Label playersLabel;

    public WaitingScreen() {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setStyle("-fx-padding: 40; -fx-background-color: #2b2b2b;");

        statusLabel = new Label("Ожидание подключения игроков...");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.WHITE);

        playersLabel = new Label("Подключено: 0/4");
        playersLabel.setFont(Font.font("Arial", 14));
        playersLabel.setTextFill(Color.LIGHTGRAY);

        getChildren().addAll(statusLabel, playersLabel);
    }

    public void updatePlayers(List<String> players) {
        if (players != null) {
            Platform.runLater(() -> {
                playersLabel.setText("Подключено: " + players.size() + "/4");
                if (players.size() >= 4) {
                    statusLabel.setText("Все игроки подключены! Начинаем игру...");
                    statusLabel.setTextFill(Color.LIGHTGREEN);
                }
            });
        }
    }
}
