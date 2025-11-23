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
    private List<Hex> ownedHexes;

    public Player(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.money = 10;
        this.income = 3;
        this.ownedHexes = new ArrayList<>();
    }

}
