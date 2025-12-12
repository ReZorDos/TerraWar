package ru.kpfu.itis.service;

import ru.kpfu.itis.model.GameMap;
import ru.kpfu.itis.model.Hex;
import ru.kpfu.itis.enums.Type;

public class MapFactory {

    /**
     * КАРТА 1: БОЛЬШОЙ МАТЕРИК С ПОЛУОСТРОВАМИ
     */
    public static GameMap createPeninsulaMap() {
        GameMap map = new GameMap(14, 13);
        int[][] peninsula = new int[][]{
                {0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0}
        };
        placeHexesFromMap(map, peninsula);
        setStartingZones(map, new int[][]{{1, 1}, {1, 2}, {2, 1}, {2, 2}, {2, 3}},
                new int[][]{{11, 10}, {11, 11}, {10, 11}, {10, 10}, {11, 9}});
        return map;
    }

    /**
     * КАРТА 2: S-ОБРАЗНЫЙ КОНТИНЕНТ
     */
    public static GameMap createSShapedMap() {
        GameMap map = new GameMap(14, 13);
        int[][] sShaped = new int[][]{
                {0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0},
                {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0}
        };
        placeHexesFromMap(map, sShaped);
        setStartingZones(map, new int[][]{{2, 1}, {3, 1}, {2, 2}, {3, 2}, {2, 3}},
                new int[][]{{9, 9}, {10, 9}, {9, 8}, {10, 8}, {8, 8}});
        return map;
    }

    /**
     * КАРТА 3: ЗВЁЗДООБРАЗНЫЙ МАТЕРИК
     */
    public static GameMap createStarShapedMap() {
        GameMap map = new GameMap(14, 13);
        int[][] star = new int[][]{
                {0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0},
                {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0}
        };
        placeHexesFromMap(map, star);
        setStartingZones(map, new int[][]{{1, 4}, {2, 3}, {1, 3}, {2, 4}, {2, 5}},
                new int[][]{{12, 4}, {11, 5}, {12, 5}, {11, 4}, {11, 3}});
        return map;
    }

    /**
     * КАРТА 4: Т-ОБРАЗНЫЙ МАТЕРИК
     */
    public static GameMap createTShapedMap() {
        GameMap map = new GameMap(14, 13);
        int[][] tShaped = new int[][]{
                {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
                {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
                {0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
                {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
        placeHexesFromMap(map, tShaped);
        setStartingZones(map, new int[][]{{2, 1}, {2, 2}, {3, 1}, {1, 1}, {3, 2}},
                new int[][]{{11, 11}, {11, 12}, {10, 11}, {12, 11}, {10, 12}});
        return map;
    }

    /**
     * КАРТА 5: С-ОБРАЗНЫЙ МАТЕРИК
     */
    public static GameMap createCShapedMap() {
        GameMap map = new GameMap(14, 13);
        int[][] cShaped = new int[][]{
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0},
                {1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0}
        };
        placeHexesFromMap(map, cShaped);
        setStartingZones(map, new int[][]{{1, 1}, {2, 1}, {1, 2}, {2, 2}, {1, 3}},
                new int[][]{{9, 10}, {9, 11}, {8, 10}, {8, 11}, {9, 9}});
        return map;
    }

    /**
     * Размещение гексов по карте из шаблона
     * 1 = гекс (трава), 0 = пусто (null)
     */
    private static void placeHexesFromMap(GameMap map, int[][] pattern) {
        for (int y = 0; y < pattern.length && y < map.getHeight(); y++) {
            for (int x = 0; x < pattern[y].length && x < map.getWidth(); x++) {
                if (pattern[y][x] == 1) {
                    // Создаём новый гекс (Трава)
                    Hex hex = new Hex(x, y, Type.GRASS);
                    map.getGrid().get(y).set(x, hex);
                } else {
                    // Если 0, то гекса быть НЕ должно -> null
                    map.getGrid().get(y).set(x, null);
                }
            }
        }
    }

    /**
     * Установка стартовых зон для двух игроков
     * Каждый игрок получает 5 гексов в начале игры
     *
     * @param map Карта игры
     * @param player0Positions Стартовые позиции для Player 0 (RED)
     * @param player1Positions Стартовые позиции для Player 1 (BLUE)
     */
    private static void setStartingZones(GameMap map, int[][] player0Positions, int[][] player1Positions) {
        // Player 0 (RED) - ID = 0
        for (int[] pos : player0Positions) {
            int x = pos[0];
            int y = pos[1];
            Hex hex = map.getHex(x, y);
            if (hex != null) {
                hex.setOwnerId(0);
            }
        }

        // Player 1 (BLUE) - ID = 1
        for (int[] pos : player1Positions) {
            int x = pos[0];
            int y = pos[1];
            Hex hex = map.getHex(x, y);
            if (hex != null) {
                hex.setOwnerId(1);
            }
        }
    }

    /**
     * Получить карту по ID (0-4)
     */
    public static GameMap getMapById(int mapId) {
        return switch (mapId) {
            case 0 -> createPeninsulaMap();
            case 1 -> createSShapedMap();
            case 2 -> createStarShapedMap();
            case 3 -> createTShapedMap();
            case 4 -> createCShapedMap();
            default -> createPeninsulaMap();
        };
    }

    /**
     * Получить название карты по ID
     */
    public static String getMapNameById(int mapId) {
        return switch (mapId) {
            case 0 -> "Материк с полуостровами";
            case 1 -> "S-образный континент";
            case 2 -> "Звёздообразный материк";
            case 3 -> "Т-образный материк";
            case 4 -> "С-образный материк";
            default -> "Неизвестная карта";
        };
    }

    /**
     * Получить описание карты по ID
     */
    public static String getMapDescriptionById(int mapId) {
        return switch (mapId) {
            case 0 -> "Единый континент с множеством полуостровов - контроль полуостровов даёт стратегическое преимущество";
            case 1 -> "Волнистый континент в форме буквы S - движение и экспансия в одном направлении";
            case 2 -> "Звёзда с пятью лучами из центра - контроль центра даёт доступ ко всем направлениям";
            case 3 -> "Две большие области, связанные узким проливом - битва за стратегический проход";
            case 4 -> "C-образный материк, как Индия - контроль побережья и внутренних регионов";
            default -> "Описание недоступно";
        };
    }

    /**
     * Получить все доступные карты
     */
    public static GameMap[] getAllMaps() {
        return new GameMap[]{
                createPeninsulaMap(),
                createSShapedMap(),
                createStarShapedMap(),
                createTShapedMap(),
                createCShapedMap()
        };
    }

    /**
     * Получить случайную карту
     */
    public static GameMap getRandomMap() {
        int randomId = (int)(Math.random() * 5);
        return getMapById(randomId);
    }
}