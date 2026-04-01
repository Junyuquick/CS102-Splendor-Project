package engine.validation;

import engine.moves.Move;
import engine.payment.PaymentCalculator;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Answers whether a player still has alternative legal moves available.
 */
final class AvailableMovesInspector {
    private final ReserveMoveRule reserveMoveRule;
    private final BuyMoveRule buyMoveRule;
    private final ReturnTokensMoveRule returnTokensMoveRule;

    AvailableMovesInspector(
            ReserveMoveRule reserveMoveRule,
            BuyMoveRule buyMoveRule,
            ReturnTokensMoveRule returnTokensMoveRule
    ) {
        this.reserveMoveRule = reserveMoveRule;
        this.buyMoveRule = buyMoveRule;
        this.returnTokensMoveRule = returnTokensMoveRule;
    }

    boolean hasAnyLegalReserve(GameState state, Player player) {
        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            if (reserveMoveRule.validate(state, player, Move.reserveFaceUp(card)) == null) {
                return true;
            }
        }

        return false;
    }

    boolean hasAnyLegalBuy(GameState state, Player player) {
        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            Move move = Move.buy(
                    card,
                    PaymentCalculator.computePaymentTokens(player, card.getCost()),
                    false
            );
            if (buyMoveRule.validate(state, player, move) == null) {
                return true;
            }
        }

        for (DevelopmentCard card : player.getReservedCards()) {
            Move move = Move.buy(
                    card,
                    PaymentCalculator.computePaymentTokens(player, card.getCost()),
                    true
            );
            if (buyMoveRule.validate(state, player, move) == null) {
                return true;
            }
        }

        return false;
    }

    boolean hasAnyLegalReturnTokens(Player player) {
        if (!returnTokensMoveRule.canUseReturnTurn(player)) {
            return false;
        }

        for (Map.Entry<GemColor, Integer> entry : player.getTokens().entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            Map<GemColor, Integer> tokens = new EnumMap<>(GemColor.class);
            tokens.put(entry.getKey(), 1);
            if (returnTokensMoveRule.validate(player, Move.returnTokens(tokens)) == null) {
                return true;
            }
        }

        return false;
    }
}
