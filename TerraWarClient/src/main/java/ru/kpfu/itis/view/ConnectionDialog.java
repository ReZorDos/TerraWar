package ru.kpfu.itis.view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.kpfu.itis.network.connection.ConnectionResult;


public class ConnectionDialog {

    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_SERVER_PORT = 5555;

    public static ConnectionResult showAndWait() {
        Stage stage = new Stage();
        stage.setTitle("Подключение к серверу");
        stage.setWidth(400);
        stage.setHeight(400);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #2b2b2b;");

        Label titleLabel = new Label("Подключение к серверу");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.WHITE);

        VBox connectionFields = new VBox(15);
        connectionFields.setAlignment(Pos.CENTER);
        connectionFields.setStyle("-fx-padding: 10;");

        Label playerNameLabel = new Label("Введите ваш никнейм:");
        playerNameLabel.setTextFill(Color.WHITE);
        playerNameLabel.setFont(Font.font("Arial", 14));
        TextField playerNameField = new TextField("Игрок");
        playerNameField.setPrefWidth(250);
        playerNameField.setPrefHeight(35);

        Label serverHostLabel = new Label("IP адрес сервера:");
        serverHostLabel.setTextFill(Color.WHITE);
        serverHostLabel.setFont(Font.font("Arial", 14));
        TextField serverHostField = new TextField(DEFAULT_SERVER_HOST);
        serverHostField.setPrefWidth(250);
        serverHostField.setPrefHeight(35);

        Label serverPortLabel = new Label("Порт сервера:");
        serverPortLabel.setTextFill(Color.WHITE);
        serverPortLabel.setFont(Font.font("Arial", 14));
        TextField serverPortField = new TextField(String.valueOf(DEFAULT_SERVER_PORT));
        serverPortField.setPrefWidth(250);
        serverPortField.setPrefHeight(35);

        connectionFields.getChildren().addAll(playerNameLabel, playerNameField, 
                                               serverHostLabel, serverHostField,
                                               serverPortLabel, serverPortField);

        ConnectionResult[] result = new ConnectionResult[1];
        
        stage.setOnCloseRequest(e -> {
            result[0] = null;
        });

        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setStyle("-fx-padding: 15;");

        Button connectButton = new Button("Подключиться");
        connectButton.setPrefWidth(140);
        connectButton.setPrefHeight(40);
        connectButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        connectButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5;"
        );

        Button cancelButton = new Button("Отмена");
        cancelButton.setPrefWidth(140);
        cancelButton.setPrefHeight(40);
        cancelButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        cancelButton.setStyle(
                "-fx-background-color: #f44336; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5;"
        );

        connectButton.setOnAction(e -> {
            String playerName = playerNameField.getText().trim();
            String serverHost = serverHostField.getText().trim();
            String serverPortText = serverPortField.getText().trim();

            if (playerName.isEmpty()) {
                return;
            }

            if (serverHost.isEmpty()) {
                serverHost = DEFAULT_SERVER_HOST;
            }

            int serverPort = DEFAULT_SERVER_PORT;
            try {
                if (!serverPortText.isEmpty()) {
                    serverPort = Integer.parseInt(serverPortText);
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText(null);
                alert.setContentText("Неверный формат порта. Используется порт по умолчанию: " + DEFAULT_SERVER_PORT);
                alert.showAndWait();
                serverPort = DEFAULT_SERVER_PORT;
            }

            result[0] = new ConnectionResult(
                    serverHost,
                    serverPort,
                    playerName,
                    -1
            );
            stage.close();
        });

        cancelButton.setOnAction(e -> {
            result[0] = null;
            stage.close();
        });

        buttonsBox.getChildren().addAll(connectButton, cancelButton);

        root.getChildren().addAll(
                titleLabel,
                connectionFields,
                buttonsBox
        );

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();

        return result[0];
    }

}
