package ru.kpfu.itis.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tower {
    private int id;
    private int ownerId;
    private int hexX;
    private int hexY;
    private int level;

    public Tower(int id, int ownerId, int hexX, int hexY, int level) {
        this.id = id;
        this.ownerId = ownerId;
        this.hexX = hexX;
        this.hexY = hexY;
        this.level = level;
    }

    public boolean canUnitPassThrough(int unitLevel) {
        if (unitLevel >= 3) {
            return true;
        }
        if (this.level == 1) {
            return unitLevel >= 2;
        }
        if (this.level == 2) {
            return unitLevel >= 3;
        }
        return true;
    }


    public boolean canUnitAttack(int unitLevel) {
        if (this.level == 1) {
            return unitLevel >= 2;
        }
        if (this.level == 2) {
            return unitLevel >= 3;
        }
        return false;
    }
}
