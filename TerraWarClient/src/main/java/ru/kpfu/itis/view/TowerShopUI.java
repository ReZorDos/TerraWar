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
import ru.kpfu.itis.service.TowerShop;

/**
 * UI диалог для покупки башен с картинками
 */
public class TowerShopUI {

    private final TowerShop towerShop;
    private final ImageCache imageCache;
    private final Runnable onTowerSelected;
    private Integer selectedLevel = null;
    private VBox selectedCard = null;

    public TowerShopUI(TowerShop towerShop, ImageCache imageCache, Runnable onTowerSelected) {
        this.towerShop = towerShop;
        this.imageCache = imageCache;
        this.onTowerSelected = onTowerSelected;
    }

    public Integer showAndWait(int playerMoney, int playerId) {
        Stage stage = new Stage();
        stage.setTitle("Покупка башни");
        stage.setWidth(800);
        stage.setHeight(550);

        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-padding: 20; -fx-background-color: #f0f0f0;");

        // Заголовок
        Label titleLabel = new Label("Выберите башню для покупки");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.BLACK);

        Label moneyLabel = new Label("Ваши деньги: " + playerMoney + " монет");
        moneyLabel.setFont(Font.font("Arial", 16));
        moneyLabel.setTextFill(Color.DARKGREEN);
        moneyLabel.setStyle("-fx-font-weight: bold;");

        // Контейнер для башен
        HBox towersBox = new HBox(25);
        towersBox.setAlignment(Pos.CENTER);
        towersBox.setStyle("-fx-padding: 25;");

        // Башня 1
        VBox tower1Box = createTowerCard(1, playerMoney, playerId);
        towersBox.getChildren().add(tower1Box);

        // Башня 2
        VBox tower2Box = createTowerCard(2, playerMoney, playerId);
        towersBox.getChildren().add(tower2Box);

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
                onTowerSelected.run();
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

        root.getChildren().addAll(titleLabel, moneyLabel, towersBox, buttonsBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();

        return selectedLevel;
    }

    private VBox createTowerCard(int level, int playerMoney, int playerId) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-border-color: #cccccc; -fx-border-width: 2; -fx-padding: 20; -fx-border-radius: 8;");
        card.setPrefWidth(200);

        // Картинка башни
        Image towerImage = imageCache.get("tower_" + level);
        ImageView imageView = new ImageView(towerImage);
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        // Название
        Label nameLabel = new Label("Башня " + level);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        // Описание эффекта
        String effect;
        if (level == 1) {
            effect = "Блокирует юниты\nуровня 1\n\nУровень блокировки: 1";
        } else {
            effect = "Блокирует юниты\nуровня 1-2\n\nУровень блокировки: 2";
        }
        Label effectLabel = new Label(effect);
        effectLabel.setFont(Font.font("Arial", 11));
        effectLabel.setTextFill(Color.DARKSLATEGRAY);
        effectLabel.setStyle("-fx-text-alignment: center; -fx-line-spacing: 2;");
        effectLabel.setWrapText(true);

        // Цена
        int price = towerShop.getTowerPrice(playerId, level);
        Label priceLabel = new Label(price + " монет");
        priceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        // Проверка доступности
        boolean canAfford = towerShop.canAffordTower(playerMoney, level, playerId);
        if (!canAfford) {
            card.setStyle("-fx-border-color: #ff6666; -fx-border-width: 2; -fx-padding: 20; -fx-border-radius: 8; -fx-opacity: 0.6;");
            priceLabel.setTextFill(Color.RED);
        } else {
            card.setStyle("-fx-border-color: #6666ff; -fx-border-width: 2; -fx-padding: 20; -fx-border-radius: 8; -fx-cursor: hand;");
            priceLabel.setTextFill(Color.DARKBLUE);

            // Кликабельность
            final int finalLevel = level;
            card.setOnMouseClicked(e -> {
                if (selectedCard != null && selectedCard != card) {
                    selectedCard.setStyle("-fx-border-color: #6666ff; -fx-border-width: 2; -fx-padding: 20; -fx-border-radius: 8; -fx-cursor: hand;");
                }

                selectedLevel = finalLevel;
                selectedCard = card;
                // Подсветка выбора
                card.setStyle("-fx-border-color: #0066ff; -fx-border-width: 3; -fx-padding: 20; -fx-border-radius: 8; -fx-background-color: #e6f2ff;");
            });
        }

        card.getChildren().addAll(imageView, nameLabel, effectLabel, priceLabel);
        return card;
    }

    public Integer getSelectedLevel() {
        return selectedLevel;
    }
}
