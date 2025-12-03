package ru.kpfu.itis.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Farm {

    private int id;
    private int ownerId;
    private int hexX;
    private int hexY;
    private static final int BASE_INCOME = 4;

    public Farm(int id, int ownerId, int hexX, int hexY) {
        this.id = id;
        this.ownerId = ownerId;
        this.hexX = hexX;
        this.hexY = hexY;
    }

    public int getIncome() {
        return BASE_INCOME;
    }

}
