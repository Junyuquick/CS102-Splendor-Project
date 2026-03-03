package ai;

import engine.Move;
import engine.MoveValidator;
import model.GameState;
import model.Player;
import ui.ConsoleUi;

public class AiController implements PlayerController {
    public enum Level {
        HIGH,
        LOW
    }

    private final GreedyStrategy strategy = new GreedyStrategy();
    private final Level level;

    public AiController() {
        this(Level.HIGH);
    }

    public AiController(Level level) {
        this.level = level;
    }

    @Override
    public Move chooseMove(GameState state, Player player, ConsoleUi ui, MoveValidator validator) {
        Move move = strategy.chooseMove(state, player, validator, level);
        ui.showMessage(player.getName() + " (Computer-" + level + ") chose: " + move);
        return move;
    }
}
