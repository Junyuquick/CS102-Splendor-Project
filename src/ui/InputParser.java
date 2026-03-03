package ui;

import engine.Move;
import model.Board;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class InputParser {

    public Move parseMove(String rawInput, GameState state, Player player) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new IllegalArgumentException("Command cannot be empty");
        }

        String[] parts = rawInput.trim().toLowerCase(Locale.ROOT).split("\\s+");
        String action = parts[0];

        return switch (action) {
            case "get", "take" -> parseGet(parts);
            case "buy" -> parseBuy(parts, state, player);
            case "reserve" -> parseReserve(parts, state);
            default -> throw new IllegalArgumentException("Unknown command: " + action);
        };
    }

    private Move parseGet(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Use: get <color> <color> <color>");
        }

        Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
        for (int i = 1; i < parts.length; i++) {
            GemColor color = parseColor(parts[i]);
            tokens.put(color, tokens.getOrDefault(color, 0) + 1);
        }

        if (tokens.size() == 3 && total(tokens) == 3) {
            return Move.takeDifferent(tokens);
        }
        if (tokens.size() == 1 && total(tokens) == 2) {
            return Move.takeSame(tokens);
        }
        throw new IllegalArgumentException("Use either 3 different colors, or 2 of the same color");
    }

    private Move parseBuy(String[] parts, GameState state, Player player) {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Use: buy a1 (board) or buy r1 (reserved)");
        }

        String ref = parts[1];
        DevelopmentCard card;
        boolean fromReserved;

        if (ref.matches("r\\d+")) {
            int idx = Integer.parseInt(ref.substring(1)) - 1;
            if (idx < 0 || idx >= player.getReservedCards().size()) {
                throw new IllegalArgumentException("Reserved card index out of range");
            }
            card = player.getReservedCards().get(idx);
            fromReserved = true;
        } else {
            CardRef cardRef = parseBoardRef(ref);
            card = resolveBoardCard(state.getBoard(), cardRef.tier(), cardRef.index());
            fromReserved = false;
        }

        Map<GemColor, Integer> payment = computePaymentTokens(player, card.getCost());
        return Move.buy(card, payment, fromReserved);
    }

    private Move parseReserve(String[] parts, GameState state) {
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("Use: reserve b2");
        }

        // Supports both "reserve b2" and "reserve reserve b2"
        String ref = parts.length == 3 ? parts[2] : parts[1];
        CardRef cardRef = parseBoardRef(ref);
        DevelopmentCard card = resolveBoardCard(state.getBoard(), cardRef.tier(), cardRef.index());
        return Move.reserveFaceUp(card);
    }

    private CardRef parseBoardRef(String ref) {
        if (!ref.matches("[a-d][1-3]")) {
            throw new IllegalArgumentException("Card reference must look like a1..d3");
        }

        int col = ref.charAt(0) - 'a';
        int tier = ref.charAt(1) - '0';
        return new CardRef(tier, col);
    }

    private DevelopmentCard resolveBoardCard(Board board, int tier, int index) {
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("Tier must be 1, 2, or 3");
        }

        if (index < 0 || index >= board.getFaceUpCards(tier).size()) {
            throw new IllegalArgumentException("No card at that board position");
        }
        return board.getFaceUpCards(tier).get(index);
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

            int availableColor = player.getTokenCount(color);
            int useColor = Math.min(availableColor, remaining);
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

    private GemColor parseColor(String raw) {
        return switch (raw) {
            case "white", "w" -> GemColor.WHITE;
            case "blue", "u", "b" -> GemColor.BLUE;
            case "green", "g" -> GemColor.GREEN;
            case "red", "r" -> GemColor.RED;
            case "black", "k" -> GemColor.BLACK;
            case "gold", "y" -> GemColor.GOLD;
            default -> throw new IllegalArgumentException("Unknown color: " + raw);
        };
    }

    private int total(Map<GemColor, Integer> map) {
        int sum = 0;
        for (int value : map.values()) {
            sum += value;
        }
        return sum;
    }

    private record CardRef(int tier, int index) {}
}
