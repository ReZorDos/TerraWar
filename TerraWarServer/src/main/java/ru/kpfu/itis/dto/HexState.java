package ru.kpfu.itis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HexState {

    private int x;
    private int y;
    private String type;
    private int ownerId;
    private int unitLevel;
    private boolean capital;

}

