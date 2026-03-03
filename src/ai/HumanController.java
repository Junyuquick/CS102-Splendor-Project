package ai;

import engine.Move;
import engine.MoveValidator;
import model.GameState;
import model.Player;
import ui.ConsoleUi;

public class HumanController implements PlayerController {
    @Override
    public Move chooseMove(GameState state, Player player, ConsoleUi ui, MoveValidator validator) {
        return ui.promptMove(state, player);
    }
}
