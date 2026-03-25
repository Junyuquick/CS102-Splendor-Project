package ai;

import config.Config;
import engine.Move;
import engine.PaymentCalculator;
import engine.MoveValidator;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Selects a legal AI move using simple heuristics for token-taking, reserving, and buying.
 */
public class GreedyStrategy {
    private final Config config;
    private final Random random = new Random();

    /**
     * Creates the strategy with access to rule configuration values.
     *
     * @param config game configuration
     */
    public GreedyStrategy(Config config) {
        this.config = config;
    }

    /**
     * Chooses one legal move for the given player.
     *
     * @param state current game state
     * @param player player whose turn is being played
     * @param validator move validator used to filter illegal options
     * @return the selected move
     */
    public Move chooseMove(GameState state, Player player, MoveValidator validator) {
        return chooseEasyMove(state, player, validator);
    }

    /**
     * Chooses among legal buys, reserves, and token-taking moves using a lightweight weighted
     * random policy.
     */
    private Move chooseEasyMove(GameState state, Player player, MoveValidator validator) {
        List<Move> buyMoves = new ArrayList<>();
        List<Move> reserveMoves = new ArrayList<>();
        List<Move> takeMoves = new ArrayList<>();

        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            Move buy = Move.buy(card, computePaymentTokens(player, card.getCost()), false);
            if (validator.validate(state, player, buy) == null) {
                buyMoves.add(buy);
            }

            Move reserve = Move.reserveFaceUp(card);
            if (validator.validate(state, player, reserve) == null) {
                reserveMoves.add(reserve);
            }
        }

        for (DevelopmentCard card : player.getReservedCards()) {
            Move buy = Move.buy(card, computePaymentTokens(player, card.getCost()), true);
            if (validator.validate(state, player, buy) == null) {
                buyMoves.add(buy);
            }
        }

        Move takeTwo = bestTakeTwoSame(state, player, validator);
        if (takeTwo != null) {
            takeMoves.add(takeTwo);
        }
        Move takeThree = bestTakeThreeDifferent(state, player, validator);
        if (takeThree != null) {
            takeMoves.add(takeThree);
        }

        int roll = random.nextInt(100);
        if (roll < 60 && !takeMoves.isEmpty()) {
            return takeMoves.get(random.nextInt(takeMoves.size()));
        }
        if (roll < 90 && !reserveMoves.isEmpty()) {
            return reserveMoves.get(random.nextInt(reserveMoves.size()));
        }
        if (!buyMoves.isEmpty()) {
            return buyMoves.get(random.nextInt(buyMoves.size()));
        }
        if (!takeMoves.isEmpty()) {
            return takeMoves.get(random.nextInt(takeMoves.size()));
        }
        if (!reserveMoves.isEmpty()) {
            return reserveMoves.get(random.nextInt(reserveMoves.size()));
        }

        throw new IllegalStateException("No legal moves available for AI");
    }

    /**
     * Returns the highest-value affordable purchase, preferring more prestige and then lower cost.
     */
    private Move bestBuyMove(GameState state, Player player, MoveValidator validator) {
        Move best = null;
        int bestPoints = Integer.MIN_VALUE;
        int bestCost = Integer.MAX_VALUE;

        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            Move move = Move.buy(card, computePaymentTokens(player, card.getCost()), false);
            if (validator.validate(state, player, move) != null) {
                continue;
            }
            int points = card.getPrestigePoints();
            int cost = totalCost(card.getCost());
            if (points > bestPoints || (points == bestPoints && cost < bestCost)) {
                best = move;
                bestPoints = points;
                bestCost = cost;
            }
        }

        for (DevelopmentCard card : player.getReservedCards()) {
            Move move = Move.buy(card, computePaymentTokens(player, card.getCost()), true);
            if (validator.validate(state, player, move) != null) {
                continue;
            }
            int points = card.getPrestigePoints();
            int cost = totalCost(card.getCost());
            if (points > bestPoints || (points == bestPoints && cost < bestCost)) {
                best = move;
                bestPoints = points;
                bestCost = cost;
            }
        }

        return best;
    }

    /**
     * Chooses the legal two-of-a-kind token move that best supports visible cards.
     */
    private Move bestTakeTwoSame(GameState state, Player player, MoveValidator validator) {
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
     * Chooses the legal take-different move with the highest heuristic score.
     */
    private Move bestTakeThreeDifferent(GameState state, Player player, MoveValidator validator) {
        List<GemColor> colors = normalColors();
        Move best = null;
        int bestScore = Integer.MIN_VALUE;

        return bestTakeThreeDifferent(state, player, validator, colors, 0, config.getTakeDifferentCount(), new ArrayList<>(), best, bestScore);
    }

    /**
     * Recursively enumerates color combinations and keeps the highest-scoring legal selection.
     */
    private Move bestTakeThreeDifferent(
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
            if (validator.validate(state, player, move) == null && score > bestScore) {
                return move;
            }
            return bestMove;
        }

        Move best = bestMove;
        int localBestScore = bestScore;
        for (int i = startIndex; i <= colors.size() - remaining; i++) {
            chosen.add(colors.get(i));
            Move candidate = bestTakeThreeDifferent(state, player, validator, colors, i + 1, remaining - 1, chosen, best, localBestScore);
            if (candidate != null) {
                int candidateScore = 0;
                for (GemColor color : candidate.getTokens().keySet()) {
                    candidateScore += neededForVisibleCards(state, player, color);
                }
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
     * Estimates how useful a token color is across the currently visible market.
     */
    private int neededForVisibleCards(GameState state, Player player, GemColor color) {
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
     * Computes the token payment the AI would use to buy a card.
     */
    private Map<GemColor, Integer> computePaymentTokens(Player player, Map<GemColor, Integer> cost) {
        return PaymentCalculator.computePaymentTokens(player, cost);
    }

    /**
     * Returns the total number of colored requirements in a card cost.
     */
    private int totalCost(Map<GemColor, Integer> cost) {
        int total = 0;
        for (int amount : cost.values()) {
            total += amount;
        }
        return total;
    }

    /**
     * Returns the non-gold gem colors in enum order.
     */
    private List<GemColor> normalColors() {
        List<GemColor> colors = new ArrayList<>();
        for (GemColor c : GemColor.values()) {
            if (c != GemColor.GOLD) {
                colors.add(c);
            }
        }
        return colors;
    }
}
