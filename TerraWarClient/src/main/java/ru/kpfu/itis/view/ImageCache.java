package ru.kpfu.itis.view;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple in-memory cache for images used on the game map.
 */
public class ImageCache {

    private final Map<String, Image> cache = new HashMap<>();

    public ImageCache() {
        loadImages();
    }

    public Image get(String key) {
        return cache.get(key);
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
        }
    }
}

