package engine;

import model.GemColor;

import java.util.HashMap;
import java.util.Map;

final class ReturnTokensMove extends Move {
    private final Map<GemColor, Integer> tokens;

    ReturnTokensMove(Map<GemColor, Integer> tokens) {
        this.tokens = new HashMap<>(tokens);
    }

    @Override
    public String getTypeName() {
        return "RETURN_TOKENS";
    }

    @Override
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>(tokens);
    }
}
