package ru.kpfu.itis.view;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class WaitingScreen extends VBox {

    private final Label statusLabel;
    private final Label playersLabel;
    private final Label readyStatusLabel;
    private final Button readyButton;
    private boolean isReady = false;
    private Consumer<Boolean> onReadyCallback;

    public WaitingScreen() {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setStyle("-fx-padding: 40; -fx-background-color: #2b2b2b;");

        statusLabel = new Label("Ожидание подключения игроков...");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        statusLabel.setTextFill(Color.WHITE);

        playersLabel = new Label("Подключено: 0/4 (минимум 2)");
        playersLabel.setFont(Font.font("Arial", 14));
        playersLabel.setTextFill(Color.LIGHTGRAY);

        readyStatusLabel = new Label("");
        readyStatusLabel.setFont(Font.font("Arial", 12));
        readyStatusLabel.setTextFill(Color.LIGHTGRAY);
        readyStatusLabel.setWrapText(true);
        readyStatusLabel.setMaxWidth(350);

        readyButton = new Button("Я готов");
        readyButton.setPrefWidth(140);
        readyButton.setPrefHeight(40);
        readyButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        readyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        readyButton.setDisable(true);
        readyButton.setVisible(false);
        
        readyButton.setOnAction(e -> {
            isReady = !isReady;
            updateReadyButton();
            if (onReadyCallback != null) {
                onReadyCallback.accept(isReady);
            }
        });

        getChildren().addAll(statusLabel, playersLabel, readyStatusLabel, readyButton);
    }

    public void setOnReadyCallback(Consumer<Boolean> callback) {
        this.onReadyCallback = callback;
    }

    private void updateReadyButton() {
        if (isReady) {
            readyButton.setText("Не готов");
            readyButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5;");
        } else {
            readyButton.setText("Я готов");
            readyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        }
    }

    public void updatePlayers(List<String> players, Map<String, Boolean> readyPlayers, String myPlayerName) {
        if (players != null) {
            Platform.runLater(() -> {
                playersLabel.setText("Подключено: " + players.size() + "/4 (минимум 2)");
                
                if (players.size() >= 2) {
                    statusLabel.setText("Достаточно игроков!");
                    statusLabel.setTextFill(Color.LIGHTGREEN);
                    readyButton.setDisable(false);
                    readyButton.setVisible(true);
                    
                    if (readyPlayers != null && myPlayerName != null) {
                        Boolean serverReadyStatus = readyPlayers.get(myPlayerName);
                        if (serverReadyStatus != null && serverReadyStatus != isReady) {
                            isReady = serverReadyStatus;
                            updateReadyButton();
                        }
                    }
                    
                    if (readyPlayers != null && !readyPlayers.isEmpty()) {
                        StringBuilder readyText = new StringBuilder("Готовность игроков:\n");
                        int readyCount = 0;
                        for (String player : players) {
                            boolean ready = readyPlayers.getOrDefault(player, false);
                            if (ready) readyCount++;
                            String status = ready ? "✓ Готов" : "○ Ожидание";
                            readyText.append(player).append(": ").append(status).append("\n");
                        }
                        readyText.append("\nГотово: ").append(readyCount).append("/").append(players.size());
                        readyStatusLabel.setText(readyText.toString());
                    } else {
                        readyStatusLabel.setText("");
                    }
                } else {
                    statusLabel.setText("Ожидание подключения игроков...");
                    statusLabel.setTextFill(Color.WHITE);
                    readyButton.setDisable(true);
                    readyButton.setVisible(false);
                    readyStatusLabel.setText("");
                }
            });
        }
    }
    
    public void updatePlayers(List<String> players) {
        updatePlayers(players, null, null);
    }
}
