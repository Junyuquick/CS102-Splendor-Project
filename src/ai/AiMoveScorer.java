package ai;

import config.Config;
import engine.moves.Move;
import engine.validation.MoveValidator;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scores the token-taking choices considered by the greedy AI.
 *
 * The heuristic is intentionally simple: prefer colors that help the
 * player move closer to buying the face-up cards on the board.
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
     * the visible market.
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
            Map<GemColor, Integer> tokens = new LinkedHashMap<>();
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
     * The scorer explores the allowed color combinations and keeps the
     * legal combination whose colors are most needed by visible cards.
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

    /**
     * Recursively explores legal take-different color combinations.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @param colors available non-gold colors
     * @param startIndex current combination start index
     * @param remaining number of colors still needed
     * @param chosen colors chosen so far
     * @param bestMove best move found so far
     * @param bestScore best score found so far
     * @return best legal move found in this search branch
     */
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
            Map<GemColor, Integer> tokens = new LinkedHashMap<>();
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

    /**
     * Scores one take move by summing how useful each chosen color is
     * against the visible market.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param candidate move to score
     * @return heuristic score for the move
     */
    private int scoreTakeMove(GameState state, Player player, Move candidate) {
        int candidateScore = 0;
        for (GemColor color : candidate.getTokens().keySet()) {
            candidateScore += neededForVisibleCards(state, player, color);
        }
        return candidateScore;
    }

    /**
     * Estimates how useful one color is across the visible cards.
     *
     * Existing bonuses and tokens are subtracted before counting the
     * remaining need.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param color color being evaluated
     * @return remaining market need for that color
     */
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

    /**
     * Returns the non-gold colors in declared color order.
     *
     * @return normal token colors
     */
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
