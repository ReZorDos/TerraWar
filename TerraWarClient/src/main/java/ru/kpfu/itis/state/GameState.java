package ru.kpfu.itis.state;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameState {
    private int currentPlayerId = -1;

    public void clearSelection() {

    }
}