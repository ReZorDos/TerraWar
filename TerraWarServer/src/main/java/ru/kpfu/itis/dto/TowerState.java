package ru.kpfu.itis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TowerState {

    private int id;
    private int ownerId;
    private int hexX;
    private int hexY;
    private int level;

}

