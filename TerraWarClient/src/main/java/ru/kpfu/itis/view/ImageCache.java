package ru.kpfu.itis.view;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

public class ImageCache {
    private final Map<String, Image> cache = new HashMap<>();

    public ImageCache() {
        loadImages();
        loadHexTextures();
    }

    public Image get(String key) {
        return cache.get(key);
    }

    private void loadImages() {
        loadSafe("unit_1.png", "unit_1");
        loadSafe("unit_2.png", "unit_2");
        loadSafe("unit_3.png", "unit_3");
        loadSafe("farm.png", "farm");
        loadSafe("tower_1.png", "tower_1");
        loadSafe("tower_2.png", "tower_2");
    }

    private void loadHexTextures() {
        Image desertTexture = loadSafe("hex_desert.png", "hex_desert");
        Image redTexture = loadSafe("hex_grass_red.png", "hex_grass_red");
        Image blueTexture = loadSafe("hex_grass_blue.png", "hex_grass_blue");
        Image yellowTexture = loadSafe("hex_grass_yellow.png", "hex_grass_yellow");
        Image pinkTexture = loadSafe("hex_grass_pink.png", "hex_grass_pink");
        loadSafe("sea_texture.png", "sea_texture");

        if (redTexture == null && desertTexture != null) {
            cache.put("hex_grass_red", desertTexture);
        }
        if (blueTexture == null && desertTexture != null) {
            cache.put("hex_grass_blue", desertTexture);
        }
        if (yellowTexture == null && desertTexture != null) {
            cache.put("hex_grass_yellow", desertTexture);
        }
        if (pinkTexture == null && desertTexture != null) {
            cache.put("hex_grass_pink", desertTexture);
        }
    }

    private Image loadSafe(String fileName, String key) {
        try {
            var is = getClass().getResourceAsStream("/" + fileName);
            if (is == null) {
                System.err.println("Не найдена картинка: " + fileName);
                return null;
            }
            Image image = new Image(is);
            if (!image.isError()) {
                cache.put(key, image);
                return image;
            } else {
                System.err.println("Ошибка загрузки " + fileName + ": " + image.getException().getMessage());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки " + fileName + ": " + e.getMessage());
            return null;
        }
    }
}