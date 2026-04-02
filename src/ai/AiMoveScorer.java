package ai;

import config.Config;
import engine.moves.Move;
import engine.validation.MoveValidator;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Scores and selects promising move candidates for the greedy AI.
 */
final class AiMoveScorer {
    private final Config config;

    /**
     * Creates a move scorer.
     *
     * @param config game configuration
     */
    AiMoveScorer(Config config) {
        this.config = config;
    }

    /**
     * Chooses the legal two-of-a-kind token move that best supports
     * visible cards.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return best legal take-two-same move, or null if none exists
     */
    Move bestTakeTwoSame(
            GameState state,
            Player player,
            MoveValidator validator
    ) {
        Move best = null;
        int bestNeedScore = Integer.MIN_VALUE;

        for (GemColor color : normalColors()) {
            Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
            tokens.put(color, config.getTakeSameCount());
            Move move = Move.takeSame(tokens);
            if (validator.validate(state, player, move) != null) {
                continue;
            }
            int need = neededForVisibleCards(state, player, color);
            if (need > bestNeedScore) {
                best = move;
                bestNeedScore = need;
            }
        }
        return best;
    }

    /**
     * Chooses the legal take-different move with the highest heuristic
     * score.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return best legal take-different move, or null if none exists
     */
    Move bestTakeDifferent(
            GameState state,
            Player player,
            MoveValidator validator
    ) {
        List<GemColor> colors = normalColors();
        return bestTakeDifferent(
                state,
                player,
                validator,
                colors,
                0,
                config.getTakeDifferentCount(),
                new ArrayList<>(),
                null,
                Integer.MIN_VALUE
        );
    }

    private Move bestTakeDifferent(
            GameState state,
            Player player,
            MoveValidator validator,
            List<GemColor> colors,
            int startIndex,
            int remaining,
            List<GemColor> chosen,
            Move bestMove,
            int bestScore
    ) {
        if (remaining == 0) {
            Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
            int score = 0;
            for (GemColor color : chosen) {
                tokens.put(color, 1);
                score += neededForVisibleCards(state, player, color);
            }
            Move move = Move.takeDifferent(tokens);
            if (validator.validate(state, player, move) == null
                    && score > bestScore) {
                return move;
            }
            return bestMove;
        }

        Move best = bestMove;
        int localBestScore = bestScore;
        for (int i = startIndex; i <= colors.size() - remaining; i++) {
            chosen.add(colors.get(i));
            Move candidate = bestTakeDifferent(
                    state,
                    player,
                    validator,
                    colors,
                    i + 1,
                    remaining - 1,
                    chosen,
                    best,
                    localBestScore
            );
            if (candidate != null) {
                int candidateScore = scoreTakeMove(state, player, candidate);
                if (candidateScore > localBestScore) {
                    best = candidate;
                    localBestScore = candidateScore;
                }
            }
            chosen.remove(chosen.size() - 1);
        }
        return best;
    }

    private int scoreTakeMove(GameState state, Player player, Move candidate) {
        int candidateScore = 0;
        for (GemColor color : candidate.getTokens().keySet()) {
            candidateScore += neededForVisibleCards(state, player, color);
        }
        return candidateScore;
    }

    private int neededForVisibleCards(
            GameState state,
            Player player,
            GemColor color
    ) {
        int need = 0;
        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            int required = card.getCost().getOrDefault(color, 0);
            int haveBonus = player.getBonusCount(color);
            int haveTokens = player.getTokenCount(color);
            need += Math.max(0, required - haveBonus - haveTokens);
        }
        return need;
    }

    private List<GemColor> normalColors() {
        List<GemColor> colors = new ArrayList<>();
        for (GemColor color : GemColor.values()) {
            if (color != GemColor.GOLD) {
                colors.add(color);
            }
        }
        return colors;
    }
}
