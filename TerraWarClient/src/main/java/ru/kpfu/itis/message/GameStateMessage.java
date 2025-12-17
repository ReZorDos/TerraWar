package ru.kpfu.itis.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.kpfu.itis.dto.FullGameState;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GameStateMessage {

    private List<String> players;
    private int currentTurn;
    private FullGameState stateSnapshot;

}

