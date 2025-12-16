package ru.kpfu.itis.view;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import ru.kpfu.itis.service.FarmShop;

/**
 * UI диалог для покупки ферм с картинками
 */
public class FarmShopUI {

    private final FarmShop farmShop;
    private final ImageCache imageCache;
    private final Runnable onFarmSelected;
    private boolean selected = false;

    public FarmShopUI(FarmShop farmShop, ImageCache imageCache, Runnable onFarmSelected) {
        this.farmShop = farmShop;
        this.imageCache = imageCache;
        this.onFarmSelected = onFarmSelected;
    }

    public boolean showAndWait(int playerMoney, int playerId) {
        Stage stage = new Stage();
        stage.setTitle("Покупка фермы");
        stage.setWidth(600);
        stage.setHeight(500);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 30; -fx-background-color: #f0f0f0;");

        // Заголовок
        Label titleLabel = new Label("Покупка фермы");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.BLACK);

        Label moneyLabel = new Label("Ваши деньги: " + playerMoney + " монет");
        moneyLabel.setFont(Font.font("Arial", 16));
        moneyLabel.setTextFill(Color.DARKGREEN);
        moneyLabel.setStyle("-fx-font-weight: bold;");

        // Контейнер для фермы
        VBox farmBox = new VBox(18);
        farmBox.setAlignment(Pos.CENTER);
        farmBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 2; -fx-padding: 25; -fx-border-radius: 8;");
        farmBox.setPrefWidth(350);

        // Картинка фермы
        Image farmImage = imageCache.get("farm");
        ImageView farmImageView = new ImageView(farmImage);
        farmImageView.setFitWidth(120);
        farmImageView.setFitHeight(120);
        farmImageView.setPreserveRatio(true);

        // Название
        Label nameLabel = new Label("Ферма");
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // Описание
        Label descLabel = new Label("Генерирует доход в конце каждого хода\nПриносит по 2 монеты за ход\nМожно строить на своей территории");
        descLabel.setFont(Font.font("Arial", 13));
        descLabel.setTextFill(Color.DARKSLATEGRAY);
        descLabel.setStyle("-fx-text-alignment: center; -fx-line-spacing: 3;");
        descLabel.setWrapText(true);

        // Цена
        int price = farmShop.getFarmPrice(playerId);
        Label priceLabel = new Label("Цена: " + price + " монет");
        priceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // Проверка доступности
        boolean canAfford = farmShop.canAffordFarm(playerMoney, playerId);
        if (!canAfford) {
            farmBox.setStyle("-fx-border-color: #ff6666; -fx-border-width: 2; -fx-padding: 25; -fx-border-radius: 8; -fx-opacity: 0.6;");
            priceLabel.setTextFill(Color.RED);
        } else {
            farmBox.setStyle("-fx-border-color: #66ff66; -fx-border-width: 2; -fx-padding: 25; -fx-border-radius: 8;");
            priceLabel.setTextFill(Color.DARKGREEN);
        }

        farmBox.getChildren().addAll(farmImageView, nameLabel, descLabel, priceLabel);

        // Кнопки подтверждения
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setStyle("-fx-padding: 20;");

        Button confirmButton = new Button("Купить");
        confirmButton.setPrefWidth(140);
        confirmButton.setPrefHeight(45);
        confirmButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        confirmButton.setStyle("-fx-padding: 10;");
        confirmButton.setDisable(!canAfford);
        confirmButton.setOnAction(e -> {
            selected = true;
            onFarmSelected.run();
            stage.close();
        });

        Button cancelButton = new Button("Отмена");
        cancelButton.setPrefWidth(140);
        cancelButton.setPrefHeight(45);
        cancelButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        cancelButton.setStyle("-fx-padding: 10;");
        cancelButton.setOnAction(e -> {
            selected = false;
            stage.close();
        });

        buttonsBox.getChildren().addAll(confirmButton, cancelButton);

        root.getChildren().addAll(titleLabel, moneyLabel, farmBox, buttonsBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();

        return selected;
    }
}
