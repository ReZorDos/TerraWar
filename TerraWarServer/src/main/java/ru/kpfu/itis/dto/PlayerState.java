package ru.kpfu.itis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerState {

    private int id;
    private String name;
    private String color;
    private int money;
    private int income;
    private int baseIncome;
    private int unitUpkeep;
    private int towerUpkeep;
    private int farmIncome;

}

