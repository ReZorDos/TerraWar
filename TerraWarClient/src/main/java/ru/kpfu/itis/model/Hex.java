package ru.kpfu.itis.model;

import lombok.*;
import ru.kpfu.itis.enums.Type;

@Data
@AllArgsConstructor
@Builder
public class Hex {

    private final int x;
    private final int y;
    private Type type;
    private int ownerId; // -1 ничейная
    private int unitLevel; // 0 - нет юнита, 1,2,3... уровень войска
    private boolean isCapital;

    public Hex(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.ownerId = -1;
        this.unitLevel = 0;
        this.isCapital = false;
    }

}