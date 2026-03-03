package engine;

import ai.PlayerController;
import config.Config;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.NobleTile;
import model.Player;
import ui.ConsoleUi;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates the Splendor game loop.
 * Rule logic stays in dedicated services; this class coordinates call order.
 */
public class GameEngine {

    private final Config config;
    private final ConsoleUi ui;
    private final GameState state;
    private final List<PlayerController> controllers;
    private final MoveValidator validator;
    private final MoveExecutor executor;
    private final NobleAssigner nobleAssigner;
    private final WinnerChecker winnerChecker;
    private final TurnManager turnManager;

    public GameEngine(Config config,
                      ConsoleUi ui,
                      GameState state,
                      List<PlayerController> controllers,
                      MoveValidator validator,
                      MoveExecutor executor,
                      NobleAssigner nobleAssigner,
                      WinnerChecker winnerChecker,
                      TurnManager turnManager) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.ui = Objects.requireNonNull(ui, "ui cannot be null");
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.validator = Objects.requireNonNull(validator, "validator cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.nobleAssigner = Objects.requireNonNull(nobleAssigner, "nobleAssigner cannot be null");
        this.winnerChecker = Objects.requireNonNull(winnerChecker, "winnerChecker cannot be null");
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager cannot be null");

        if (controllers == null || controllers.isEmpty()) {
            throw new IllegalArgumentException("controllers cannot be null/empty");
        }
        if (controllers.size() != state.getPlayers().size()) {
            throw new IllegalArgumentException("controllers size must match number of players");
        }
        this.controllers = new ArrayList<>(controllers);
    }

    public void run() {
        boolean gameOver = false;

        while (!gameOver) {
            ui.renderState(state);

            Player player = state.getCurrentPlayer();
            PlayerController controller = controllers.get(state.getCurrentPlayerIndex());
            Move legalMove = requestLegalMove(controller, player);

            executor.execute(state, player, legalMove);
            resolveTokenCapIfNeeded(player);
            resolveNobleAttraction(player);

            if (winnerChecker.shouldTriggerFinalRound(state)) {
                turnManager.markFinalRound(state);
                ui.showMessage("Final round triggered by " + player.getName());
            }

            turnManager.advanceTurn(state);
            gameOver = turnManager.hasFinalRoundCompleted(state);
        }

        Player winner = winnerChecker.determineWinner(state);
        ui.showMessage("Game over. Winner: " + winner.getName() +
                " (" + winner.getPrestigePoints() + " points)");
    }

    private Move requestLegalMove(PlayerController controller, Player player) {
        while (true) {
            Move proposed = controller.chooseMove(state, player, ui, validator);
            String validationError = validator.validate(state, player, proposed);
            if (validationError == null) {
                return proposed;
            }
            ui.showError("Illegal move: " + validationError);
        }
    }

    private void resolveTokenCapIfNeeded(Player player) {
        int maxTokens = config.getMaxTokensPerPlayer();
        while (player.getTotalTokens() > maxTokens) {
            int excess = player.getTotalTokens() - maxTokens;
            Map<GemColor, Integer> discard = ui.chooseTokensToDiscard(player, excess, state);
            if (discard.isEmpty()) {
                discard = autoDiscard(player, excess);
            }

            player.removeTokens(discard);
            GemBank bank = state.getBank();
            bank.addTokens(discard);
            ui.showMessage(player.getName() + " discarded " + discard);
        }
    }

    private void resolveNobleAttraction(Player player) {
        List<NobleTile> eligible = nobleAssigner.findEligibleNobles(state, player);
        if (eligible.isEmpty()) {
            return;
        }

        if (eligible.size() == 1) {
            NobleTile assigned = eligible.get(0);
            nobleAssigner.assignNoble(state, player, assigned);
            ui.showMessage(player.getName() + " attracted noble: " + assigned);
            return;
        }

        NobleTile chosen = ui.chooseNoble(player, eligible, state);
        if (chosen == null || !eligible.contains(chosen)) {
            chosen = eligible.get(0);
        }
        nobleAssigner.assignNoble(state, player, chosen);
        ui.showMessage(player.getName() + " chose noble: " + chosen);
    }

    private Map<GemColor, Integer> autoDiscard(Player player, int excess) {
        Map<GemColor, Integer> discard = new EnumMap<>(GemColor.class);
        Map<GemColor, Integer> working = new EnumMap<>(GemColor.class);
        working.putAll(player.getTokens());

        while (excess > 0) {
            GemColor candidate = null;
            int bestCount = 0;
            for (Map.Entry<GemColor, Integer> entry : working.entrySet()) {
                if (entry.getValue() > bestCount) {
                    candidate = entry.getKey();
                    bestCount = entry.getValue();
                }
            }
            if (candidate == null || bestCount <= 0) {
                break;
            }
            discard.put(candidate, discard.getOrDefault(candidate, 0) + 1);
            working.put(candidate, bestCount - 1);
            excess--;
        }
        return discard;
    }
}
