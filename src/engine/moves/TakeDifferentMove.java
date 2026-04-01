package engine.moves;

import model.GemColor;

import java.util.HashMap;
import java.util.Map;

public final class TakeDifferentMove extends Move {
    private final Map<GemColor, Integer> tokens;

    TakeDifferentMove(Map<GemColor, Integer> tokens) {
        this.tokens = new HashMap<>(tokens);
    }

    @Override
    public String getTypeName() {
        return "TAKE_DIFFERENT";
    }

    @Override
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>(tokens);
    }
}
