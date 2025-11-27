package ru.kpfu.itis.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Unit {
    private int id;
    private int ownerId;
    private int hexX;
    private int hexY;
    private int level;
    private int actionRadius;
    private boolean hasActed;

    public Unit(int id, int ownerId, int hexX, int hexY, int level) {
        this.id = id;
        this.ownerId = ownerId;
        this.hexX = hexX;
        this.hexY = hexY;
        this.level = level;
        this.hasActed = false;

        switch (level) {
            case 1:
                this.actionRadius = 1;
                break;
            case 2:
                this.actionRadius = 2;
                break;
            case 3:
                this.actionRadius = 3;
                break;
            default:
                this.actionRadius = 1;
        }
    }

    public void resetTurnActions() {
        this.hasActed = false;
    }

    public boolean canAct() {
        return !hasActed;
    }

    public void act() {
        this.hasActed = true;
    }

    public String getUnitTypeName() {
        return switch (level) {
            case 1 -> "Пехота";
            case 2 -> "Конница";
            case 3 -> "Артиллерия";
            default -> "Юнит";
        };
    }

    public boolean canDefeat(Unit defender) {
        return this.level >= defender.getLevel();
    }

}