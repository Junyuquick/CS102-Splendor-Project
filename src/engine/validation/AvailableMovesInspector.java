package engine.validation;

import engine.moves.Move;
import engine.payment.PaymentCalculator;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.LinkedHashMap;
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
            if (canReserveCard(state, player, card)) {
                return true;
            }
        }

        return false;
    }

    boolean hasAnyLegalBuy(GameState state, Player player) {
        for (DevelopmentCard card : state.getBoard().getFaceUpCards()) {
            if (canBuyCard(state, player, card, false)) {
                return true;
            }
        }

        for (DevelopmentCard card : player.getReservedCards()) {
            if (canBuyCard(state, player, card, true)) {
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
            if (canReturnOneToken(player, entry)) {
                return true;
            }
        }

        return false;
    }

    private boolean canReserveCard(
            GameState state,
            Player player,
            DevelopmentCard card
    ) {
        Move move = Move.reserveFaceUp(card);
        return reserveMoveRule.validate(state, player, move) == null;
    }

    private boolean canBuyCard(
            GameState state,
            Player player,
            DevelopmentCard card,
            boolean fromReserved
    ) {
        Move move = createBuyMove(player, card, fromReserved);
        return buyMoveRule.validate(state, player, move) == null;
    }

    private Move createBuyMove(
            Player player,
            DevelopmentCard card,
            boolean fromReserved
    ) {
        Map<GemColor, Integer> payment = PaymentCalculator.computePaymentTokens(
                player,
                card.getCost()
        );
        return Move.buy(card, payment, fromReserved);
    }

    private boolean canReturnOneToken(
            Player player,
            Map.Entry<GemColor, Integer> entry
    ) {
        if (entry.getValue() <= 0) {
            return false;
        }

            Map<GemColor, Integer> tokens = new LinkedHashMap<>();
        tokens.put(entry.getKey(), 1);
        Move move = Move.returnTokens(tokens);
        return returnTokensMoveRule.validate(player, move) == null;
    }
}
