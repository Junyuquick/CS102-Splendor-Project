package engine.validation;

import config.Config;
import engine.moves.Move;
import model.Board;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.Player;

import java.util.List;

/**
 * Validates reserve moves.
 */
final class ReserveMoveRule {
    private final Config config;

    ReserveMoveRule(Config config) {
        this.config = config;
    }

    String validate(GameState state, Player player, Move move) {
        DevelopmentCard card = move.getCard();

        if (card == null) {
            return "RESERVE requires a card";
        }

        Board board = state.getBoard();
        int cardLevel = move.getCardLevel();

        if (cardLevel == -1) {
            List<DevelopmentCard> faceUp = board.getFaceUpCards();
            if (!faceUp.contains(card)) {
                return "Card is not face-up on the board";
            }
        } else if (cardLevel < 1 || cardLevel > config.getNumLevels()) {
            return "Invalid reserve deck level: " + cardLevel;
        }

        List<DevelopmentCard> reserved = player.getReservedCards();
        if (reserved.size() >= config.getMaxReservedCards()) {
            return "Player has already reserved " + reserved.size()
                    + " cards (max: " + config.getMaxReservedCards() + ")";
        }

        if (state.getBank().getTokenCount(GemColor.GOLD) > 0) {
            // Reserving is still allowed even if there is no gold token to take.
        }

        return null;
    }
}
