package ui;

import engine.Move;
import model.GameState;
import model.GemColor;
import model.NobleTile;
import model.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class ConsoleUi {
    private final Scanner scanner;
    private final Printer printer;
    private final InputParser inputParser;

    public ConsoleUi() {
        this(new Scanner(System.in), new Printer(), new InputParser());
    }

    public ConsoleUi(Scanner scanner, Printer printer, InputParser inputParser) {
        this.scanner = scanner;
        this.printer = printer;
        this.inputParser = inputParser;
    }

    public void renderState(GameState state) {
        printer.printState(state);
    }

    public void showMessage(String message) {
        printer.printMessage(message);
    }

    public void showError(String message) {
        printer.printError(message);
    }

    public Move promptMove(GameState state, Player player) {
        while (true) {
            printer.printMessage("");
            printer.printMessage(player.getName() + ", enter move:");
            printer.printMessage("  get red blue green");
            printer.printMessage("  get red red");
            printer.printMessage("  buy a1");
            printer.printMessage("  reserve b2");
            printer.printMessage("  buy r1   (buy your 1st reserved card)");
            if (!scanner.hasNextLine()) {
                throw new IllegalStateException("Input closed");
            }
            String raw = scanner.nextLine();
            if ("quit".equalsIgnoreCase(raw.trim())) {
                throw new IllegalStateException("Game exited by user");
            }

            try {
                return inputParser.parseMove(raw, state, player);
            } catch (IllegalArgumentException e) {
                printer.printError(e.getMessage());
            }
        }
    }

    public Map<GemColor, Integer> chooseTokensToDiscard(Player player, int excess, GameState state) {
        Map<GemColor, Integer> discard = new EnumMap<>(GemColor.class);

        while (true) {
            printer.printMessage(player.getName() + ", discard " + excess + " token(s).");
            printer.printMessage("Format: red blue gold");
            if (!scanner.hasNextLine()) {
                throw new IllegalStateException("Input closed");
            }
            String raw = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (raw.isBlank()) {
                return discard;
            }

            String[] parts = raw.split("\\s+");
            if (parts.length != excess) {
                printer.printError("You must enter exactly " + excess + " colors.");
                continue;
            }

            discard.clear();
            boolean ok = true;
            for (String part : parts) {
                GemColor color;
                try {
                    color = parseColor(part);
                } catch (IllegalArgumentException ex) {
                    printer.printError(ex.getMessage());
                    ok = false;
                    break;
                }
                discard.put(color, discard.getOrDefault(color, 0) + 1);
            }
            if (!ok) {
                continue;
            }

            for (Map.Entry<GemColor, Integer> entry : discard.entrySet()) {
                if (player.getTokenCount(entry.getKey()) < entry.getValue()) {
                    printer.printError("You do not have enough " + entry.getKey() + " tokens to discard.");
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return discard;
            }
        }
    }

    public NobleTile chooseNoble(Player player, List<NobleTile> eligible, GameState state) {
        while (true) {
            printer.printMessage(player.getName() + ", choose a noble:");
            for (int i = 0; i < eligible.size(); i++) {
                printer.printMessage("  " + (i + 1) + ". " + eligible.get(i));
            }
            if (!scanner.hasNextLine()) {
                throw new IllegalStateException("Input closed");
            }
            String raw = scanner.nextLine().trim();
            try {
                int idx = Integer.parseInt(raw) - 1;
                if (idx < 0 || idx >= eligible.size()) {
                    printer.printError("Choice out of range");
                    continue;
                }
                return eligible.get(idx);
            } catch (NumberFormatException e) {
                printer.printError("Enter a number");
            }
        }
    }

    public String promptText(String prompt) {
        printer.printMessage(prompt);
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("Input closed");
        }
        return scanner.nextLine();
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
}
