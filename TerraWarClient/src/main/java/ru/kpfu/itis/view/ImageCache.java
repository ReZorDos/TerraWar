package ru.kpfu.itis.view;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple in-memory cache for images used on the game map.
 * FIXED: hex_grass → hex_dessert, пустые клетки → sea_texture
 */
public class ImageCache {

    private final Map<String, Image> cache = new HashMap<>();

    public ImageCache() {
        loadImages();
        loadHexTextures();
    }

    public Image get(String key) {
        Image img = cache.get(key);
        if (img == null) {
            System.err.println("⚠️ Изображение не найдено: " + key);
        }
        return img;
    }

    private void loadImages() {
        try {
            cache.put("unit_1", new Image(getClass().getResourceAsStream("/unit_1.png")));
            cache.put("unit_2", new Image(getClass().getResourceAsStream("/unit_2.png")));
            cache.put("unit_3", new Image(getClass().getResourceAsStream("/unit_3.png")));
            cache.put("farm", new Image(getClass().getResourceAsStream("/farm.png")));
            cache.put("tower_1", new Image(getClass().getResourceAsStream("/tower_1.png")));
            cache.put("tower_2", new Image(getClass().getResourceAsStream("/tower_2.png")));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображений: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadHexTextures() {
        try {
            // ✅ Пустыня - для нейтральных гексов по умолчанию (вместо hex_grass)
            Image desertTexture = new Image(getClass().getResourceAsStream("/hex_dessert.png"));
            if (desertTexture.isError()) {
                System.err.println("❌ Ошибка загрузки hex_dessert.png: " + desertTexture.getException());
            } else {
                System.out.println("✓ Текстура hex_dessert загружена: " +
                        (int)desertTexture.getWidth() + "x" + (int)desertTexture.getHeight());
            }

            // Красная трава для Player 0
            Image redTexture = new Image(getClass().getResourceAsStream("/hex_grass_red.png"));
            if (redTexture.isError()) {
                System.err.println("❌ Ошибка загрузки hex_grass_red.png: " + redTexture.getException());
                // Fallback на пустыню
                redTexture = desertTexture;
            } else {
                System.out.println("✓ Текстура hex_grass_red загружена: " +
                        (int)redTexture.getWidth() + "x" + (int)redTexture.getHeight());
            }

            // Синяя трава для Player 1
            Image blueTexture = new Image(getClass().getResourceAsStream("/hex_grass_blue.png"));
            if (blueTexture.isError()) {
                System.err.println("❌ Ошибка загрузки hex_grass_blue.png: " + blueTexture.getException());
                // Fallback на пустыню
                blueTexture = desertTexture;
            } else {
                System.out.println("✓ Текстура hex_grass_blue загружена: " +
                        (int)blueTexture.getWidth() + "x" + (int)blueTexture.getHeight());
            }

            // ✅ МОРЕ - текстура для пустых клеток (null в гриде)
            Image seaTexture = new Image(getClass().getResourceAsStream("/sea_texture.png"));
            if (seaTexture.isError()) {
                System.err.println("❌ Ошибка загрузки sea_texture.png: " + seaTexture.getException());
                // Fallback на пустыню если моря нет
                seaTexture = desertTexture;
            } else {
                System.out.println("✓ Текстура sea_texture загружена: " +
                        (int)seaTexture.getWidth() + "x" + (int)seaTexture.getHeight());
            }

            // Добавляем в кэш
            cache.put("hex_desert", desertTexture);         // Нейтральная пустыня (вместо hex_grass)
            cache.put("hex_grass_red", redTexture);         // Красная трава (Player 0)
            cache.put("hex_grass_blue", blueTexture);       // Синяя трава (Player 1)
            cache.put("sea_texture", seaTexture);           // Море для пустых клеток

            System.out.println("✓ Все текстуры гексов загружены успешно");

        } catch (Exception e) {
            System.err.println("❌ Ошибка загрузки текстур гексов: " + e.getMessage());
            e.printStackTrace();
        }
    }
}