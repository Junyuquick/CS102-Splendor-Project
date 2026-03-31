package engine;

import model.GemColor;

import java.util.HashMap;
import java.util.Map;

final class TakeDifferentMove extends Move {
    private final Map<GemColor, Integer> tokens;

    TakeDifferentMove(Map<GemColor, Integer> tokens) {
        this.tokens = new HashMap<>(tokens);
    }

    @Override
    public String getTypeName() {
        return "TAKE_THREE_DIFFERENT";
    }

    @Override
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>(tokens);
    }
}
