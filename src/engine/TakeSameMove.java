package engine;

import model.GemColor;

import java.util.HashMap;
import java.util.Map;

final class TakeSameMove extends Move {
    private final Map<GemColor, Integer> tokens;

    TakeSameMove(Map<GemColor, Integer> tokens) {
        this.tokens = new HashMap<>(tokens);
    }

    @Override
    public String getTypeName() {
        return "TAKE_TWO_SAME";
    }

    @Override
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>(tokens);
    }
}
