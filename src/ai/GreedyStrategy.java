package ai;

import engine.Move;
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

public class GreedyStrategy {
    private final Random random = new Random();

    public Move chooseMove(GameState state, Player player, MoveValidator validator) {
        return chooseEasyMove(state, player, validator);
    }

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

    private Move bestTakeTwoSame(GameState state, Player player, MoveValidator validator) {
        Move best = null;
        int bestNeedScore = Integer.MIN_VALUE;

        for (GemColor color : normalColors()) {
            Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
            tokens.put(color, 2);
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

    private Move bestTakeThreeDifferent(GameState state, Player player, MoveValidator validator) {
        List<GemColor> colors = normalColors();
        Move best = null;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < colors.size(); i++) {
            for (int j = i + 1; j < colors.size(); j++) {
                for (int k = j + 1; k < colors.size(); k++) {
                    Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
                    tokens.put(colors.get(i), 1);
                    tokens.put(colors.get(j), 1);
                    tokens.put(colors.get(k), 1);
                    Move move = Move.takeDifferent(tokens);
                    if (validator.validate(state, player, move) != null) {
                        continue;
                    }
                    int score = neededForVisibleCards(state, player, colors.get(i))
                            + neededForVisibleCards(state, player, colors.get(j))
                            + neededForVisibleCards(state, player, colors.get(k));
                    if (score > bestScore) {
                        best = move;
                        bestScore = score;
                    }
                }
            }
        }
        return best;
    }

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

    private Map<GemColor, Integer> computePaymentTokens(Player player, Map<GemColor, Integer> cost) {
        Map<GemColor, Integer> payment = new EnumMap<>(GemColor.class);
        int goldNeeded = 0;

        for (Map.Entry<GemColor, Integer> entry : cost.entrySet()) {
            GemColor color = entry.getKey();
            int required = entry.getValue();

            int remaining = Math.max(0, required - player.getBonusCount(color));
            if (remaining == 0) {
                continue;
            }

            int useColor = Math.min(player.getTokenCount(color), remaining);
            if (useColor > 0) {
                payment.put(color, useColor);
            }
            goldNeeded += (remaining - useColor);
        }

        if (goldNeeded > 0) {
            payment.put(GemColor.GOLD, goldNeeded);
        }
        return payment;
    }

    private int totalCost(Map<GemColor, Integer> cost) {
        int total = 0;
        for (int amount : cost.values()) {
            total += amount;
        }
        return total;
    }

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
