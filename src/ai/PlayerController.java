package ai;

import engine.Move;
import engine.MoveValidator;
import model.GameState;
import model.Player;
import ui.ConsoleUi;

public interface PlayerController {
    Move chooseMove(GameState state, Player player, ConsoleUi ui, MoveValidator validator);
}
