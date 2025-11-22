package ru.kpfu.itis.state;

import lombok.Getter;
import lombok.Setter;
import ru.kpfu.itis.model.Hex;
import ru.kpfu.itis.view.Hexagon;

import java.util.List;

@Getter
@Setter
public class GameState {

    private Hexagon selectedHexagon;
    private List<Hex> highlightedNeighbors;
    private int selectedOwnerId = -1;

    public void clearSelection() {
        if (selectedHexagon != null) {
            selectedHexagon.setSelected(false);
        }
        clearHighlights();
        selectedHexagon = null;
        selectedOwnerId = -1;
    }

    public void clearHighlights() {
        if (highlightedNeighbors != null) {
            highlightedNeighbors = null;
        }
    }

    public boolean isHighlightedNeighbor(Hexagon hexagon) {
        if (highlightedNeighbors == null) return false;
        return highlightedNeighbors.stream()
                .anyMatch(neighbor -> neighbor.getX() == hexagon.getGridX()
                        && neighbor.getY() == hexagon.getGridY());
    }
}