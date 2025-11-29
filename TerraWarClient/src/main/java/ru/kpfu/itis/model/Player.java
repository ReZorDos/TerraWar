package ru.kpfu.itis.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Player {

    private final int id;
    private final String name;
    private final String color;
    private int money;
    private int income;
    private int baseIncome;
    private int unitUpkeep;
    private List<Hex> ownedHexes;

    public Player(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.money = 50;
        this.baseIncome = 10;
        this.unitUpkeep = 0;
        this.income = baseIncome - unitUpkeep;
        this.ownedHexes = new ArrayList<>();
    }


    public void addUnitUpkeep(int upkeep) {
        this.unitUpkeep += upkeep;
        this.income = baseIncome - unitUpkeep;
    }

    public void removeUnitUpkeep(int upkeep) {
        this.unitUpkeep -= upkeep;
        this.income = baseIncome - unitUpkeep;
    }


}