package engine.validation;

import config.Config;
import engine.moves.BuyMove;
import engine.moves.Move;
import engine.moves.PassMove;
import engine.moves.ReserveMove;
import engine.moves.ReturnTokensMove;
import engine.moves.TakeDifferentMove;
import engine.moves.TakeSameMove;
import model.GameState;
import model.Player;

/**
 * Checks whether a move is legal before anything in the game state changes.
 *
 * If a move is valid, this class returns {@code null}. If not, it returns a message
 * explaining what was wrong.
 */
public class MoveValidator {
    private final TakeDifferentMoveRule takeDifferentMoveRule;
    private final TakeSameMoveRule takeSameMoveRule;
    private final ReserveMoveRule reserveMoveRule;
    private final BuyMoveRule buyMoveRule;
    private final ReturnTokensMoveRule returnTokensMoveRule;
    private final PassMoveRule passMoveRule;

    /**
     * Creates a validator that uses the current game rules.
     *
     * @param config game settings that contain the numeric move limits
     */
    public MoveValidator(Config config) {
        this.reserveMoveRule = new ReserveMoveRule(config);
        this.buyMoveRule = new BuyMoveRule();
        this.returnTokensMoveRule = new ReturnTokensMoveRule(config);
        AvailableMovesInspector availableMovesInspector = new AvailableMovesInspector(
                reserveMoveRule,
                buyMoveRule,
                returnTokensMoveRule
        );
        this.takeDifferentMoveRule = new TakeDifferentMoveRule(
                config,
                availableMovesInspector
        );
        this.takeSameMoveRule = new TakeSameMoveRule(config);
        this.passMoveRule = new PassMoveRule();
    }

    /**
     * Checks whether the given move is allowed right now.
     *
     * @param state the current game state
     * @param player the player making the move
     * @param move the move to check
     * @return {@code null} if the move is legal, otherwise an error message
     */
    public String validate(GameState state, Player player, Move move) {
        if (move == null) {
            return "Move cannot be null";
        }

        return validateKnownMove(state, player, move);
    }
    
    /**
     * Convenience helper that turns the validation result into a simple true or false.
     *
     * @param state the game state
     * @param player the player making the move
     * @param move the move to check
     * @return true if the move is legal, false otherwise
     */
    public boolean isLegal(GameState state, Player player, Move move) {
        return validate(state, player, move) == null;
    }

    private String validateKnownMove(
            GameState state,
            Player player,
            Move move
    ) {
        if (isTakeDifferentMove(move)) {
            return takeDifferentMoveRule.validate(state, player, move);
        }
        if (isTakeSameMove(move)) {
            return takeSameMoveRule.validate(state, player, move);
        }
        if (isReserveMove(move)) {
            return reserveMoveRule.validate(state, player, move);
        }
        if (isBuyMove(move)) {
            return buyMoveRule.validate(state, player, move);
        }
        if (isReturnTokensMove(move)) {
            return returnTokensMoveRule.validate(player, move);
        }
        if (isPassMove(move)) {
            return passMoveRule.validate();
        }

        return buildUnknownMoveMessage(move);
    }

    private boolean isTakeDifferentMove(Move move) {
        return move instanceof TakeDifferentMove;
    }

    private boolean isTakeSameMove(Move move) {
        return move instanceof TakeSameMove;
    }

    private boolean isReserveMove(Move move) {
        return move instanceof ReserveMove;
    }

    private boolean isBuyMove(Move move) {
        return move instanceof BuyMove;
    }

    private boolean isReturnTokensMove(Move move) {
        return move instanceof ReturnTokensMove;
    }

    private boolean isPassMove(Move move) {
        return move instanceof PassMove;
    }

    private String buildUnknownMoveMessage(Move move) {
        return "Unknown move type: " + move.getTypeName();
    }
}
