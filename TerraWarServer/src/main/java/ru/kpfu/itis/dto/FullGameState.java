package ru.kpfu.itis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FullGameState {

    private int mapWidth;
    private int mapHeight;
    private List<HexState> hexes;
    private List<UnitState> units;
    private List<TowerState> towers;
    private List<FarmState> farms;
    private List<PlayerState> playersState;

}


