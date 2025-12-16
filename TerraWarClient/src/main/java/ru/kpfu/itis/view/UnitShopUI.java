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
import ru.kpfu.itis.service.UnitShop;

/**
 * UI диалог для покупки юнитов с картинками
 */
public class UnitShopUI {

    private final UnitShop unitShop;
    private final ImageCache imageCache;
    private final Runnable onUnitSelected;
    private Integer selectedLevel = null;
    private VBox selectedCard = null;

    public UnitShopUI(UnitShop unitShop, ImageCache imageCache, Runnable onUnitSelected) {
        this.unitShop = unitShop;
        this.imageCache = imageCache;
        this.onUnitSelected = onUnitSelected;
    }

    public Integer showAndWait(int playerMoney) {
        Stage stage = new Stage();
        stage.setTitle("Покупка юнита");
        stage.setWidth(800);
        stage.setHeight(550);

        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-padding: 20; -fx-background-color: #f0f0f0;");

        // Заголовок
        Label titleLabel = new Label("Выберите юнита для покупки");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.BLACK);

        Label moneyLabel = new Label("Ваши деньги: " + playerMoney + " монет");
        moneyLabel.setFont(Font.font("Arial", 16));
        moneyLabel.setTextFill(Color.DARKGREEN);
        moneyLabel.setStyle("-fx-font-weight: bold;");

        // Контейнер для юнитов
        HBox unitsBox = new HBox(25);
        unitsBox.setAlignment(Pos.CENTER);
        unitsBox.setStyle("-fx-padding: 25;");

        // Юнит 1
        VBox unit1Box = createUnitCard(1, playerMoney);
        unitsBox.getChildren().add(unit1Box);

        // Юнит 2
        VBox unit2Box = createUnitCard(2, playerMoney);
        unitsBox.getChildren().add(unit2Box);

        // Юнит 3
        VBox unit3Box = createUnitCard(3, playerMoney);
        unitsBox.getChildren().add(unit3Box);

        // Кнопки подтверждения
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setStyle("-fx-padding: 20;");

        Button confirmButton = new Button("Подтвердить");
        confirmButton.setPrefWidth(140);
        confirmButton.setPrefHeight(45);
        confirmButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        confirmButton.setStyle("-fx-padding: 10;");
        confirmButton.setOnAction(e -> {
            if (selectedLevel != null) {
                onUnitSelected.run();
                stage.close();
            }
        });

        Button cancelButton = new Button("Отмена");
        cancelButton.setPrefWidth(140);
        cancelButton.setPrefHeight(45);
        cancelButton.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        cancelButton.setStyle("-fx-padding: 10;");
        cancelButton.setOnAction(e -> {
            selectedLevel = null;
            stage.close();
        });

        buttonsBox.getChildren().addAll(confirmButton, cancelButton);

        root.getChildren().addAll(titleLabel, moneyLabel, unitsBox, buttonsBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();

        return selectedLevel;
    }

    private VBox createUnitCard(int level, int playerMoney) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-border-color: #cccccc; -fx-border-width: 2; -fx-padding: 20; -fx-border-radius: 8;");
        card.setPrefWidth(200);

        // Картинка юнита
        Image unitImage = imageCache.get("unit_" + level);
        ImageView imageView = new ImageView(unitImage);
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        // Название
        Label nameLabel = new Label("Юнит " + level);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // Описание
        String description;
        if (level == 1) {
            description = "Слабый боец\nДемонстрирует базовые\nнавыки боя";
        } else if (level == 2) {
            description = "Опытный боец\nУлучшенные\nтактические навыки";
        } else {
            description = "Элитный боец\nМастер боевых\nискусств";
        }
        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Arial", 11));
        descLabel.setTextFill(Color.DARKSLATEGRAY);
        descLabel.setStyle("-fx-text-alignment: center; -fx-line-spacing: 3;");
        descLabel.setWrapText(true);

        // Цена
        int price = unitShop.getUnitPrice(level);
        Label priceLabel = new Label(price + " монет");
        priceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Проверка доступности
        boolean canAfford = unitShop.canAffordUnit(playerMoney, level);
        if (!canAfford) {
            card.setStyle("-fx-border-color: #ff6666; -fx-border-width: 2; -fx-padding: 20; -fx-border-radius: 8; -fx-opacity: 0.6;");
            priceLabel.setTextFill(Color.RED);
        } else {
            card.setStyle("-fx-border-color: #66ff66; -fx-border-width: 2; -fx-padding: 20; -fx-border-radius: 8; -fx-cursor: hand;");
            priceLabel.setTextFill(Color.DARKGREEN);

            final int finalLevel = level;
            card.setOnMouseClicked(e -> {
                if (selectedCard != null && selectedCard != card) {
                    selectedCard.setStyle("-fx-border-color: #66ff66; -fx-border-width: 2; -fx-padding: 20; -fx-border-radius: 8; -fx-cursor: hand;");
                }

                selectedLevel = finalLevel;
                selectedCard = card;
                card.setStyle("-fx-border-color: #0066ff; -fx-border-width: 3; -fx-padding: 20; -fx-border-radius: 8; -fx-background-color: #e6f2ff;");
            });
        }

        card.getChildren().addAll(imageView, nameLabel, descLabel, priceLabel);
        return card;
    }

    public Integer getSelectedLevel() {
        return selectedLevel;
    }
}