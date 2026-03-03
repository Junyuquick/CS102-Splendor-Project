package ui;

import model.Board;
import model.DevelopmentCard;
import model.GameState;
import model.GemColor;
import model.NobleTile;
import model.Player;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Printer {

    public void printState(GameState state) {
        printBoard(state.getBoard());
        printBank(state);
        printPlayers(state);
        System.out.println();
        System.out.println("Current player: " + state.getCurrentPlayer().getName());
    }

    public void printMessage(String message) {
        System.out.println(message);
    }

    public void printError(String message) {
        System.err.println(message);
    }

    private void printBoard(Board board) {
        System.out.println();
        System.out.println("=== BOARD ===");
        System.out.println("          a                       b                       c                       d");
        for (int tier = 1; tier <= 3; tier++) {
            List<DevelopmentCard> cards = board.getFaceUpCards(tier);

            List<List<String>> boxes = new ArrayList<>();
            for (int col = 0; col < 4; col++) {
                if (col < cards.size()) {
                    boxes.add(formatCardBox(cards.get(col), (char) ('a' + col), tier));
                } else {
                    boxes.add(formatEmptyBox((char) ('a' + col), tier));
                }
            }

            System.out.println("tier " + tier);
            for (int line = 0; line < boxes.get(0).size(); line++) {
                StringBuilder rowLine = new StringBuilder();
                for (int col = 0; col < 4; col++) {
                    rowLine.append(boxes.get(col).get(line)).append("  ");
                }
                System.out.println(rowLine);
            }
        }

        System.out.println("Nobles:");
        List<NobleTile> nobles = board.getAvailableNobles();
        if (nobles.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (int i = 0; i < nobles.size(); i++) {
                NobleTile noble = nobles.get(i);
                System.out.println("  " + (i + 1) + ". " + formatNoble(noble));
            }
        }
    }

    private List<String> formatCardBox(DevelopmentCard card, char col, int tier) {
        List<String> lines = new ArrayList<>();
        lines.add("+----------------------+");
        lines.add(contentLine("Colour: " + fullColor(card.getBonusColor())));
        lines.add(contentLine("Points: " + card.getPrestigePoints()));
        lines.add(contentLine("Cost:"));

        List<String> costLines = new ArrayList<>();
        for (Map.Entry<GemColor, Integer> e : card.getCost().entrySet()) {
            costLines.add(e.getValue() + " " + fullColor(e.getKey()));
        }
        if (costLines.isEmpty()) {
            lines.add(contentLine("-"));
        } else {
            for (String costLine : costLines) {
                lines.add(contentLine(costLine));
            }
        }

        // Keep a fixed minimum height so row boxes stay aligned.
        while (lines.size() < 10) {
            lines.add(contentLine(""));
        }

        lines.add("+----------------------+");
        return lines;
    }

    private List<String> formatEmptyBox(char col, int tier) {
        List<String> lines = new ArrayList<>();
        lines.add("+----------------------+");
        lines.add(contentLine("Colour: -"));
        lines.add(contentLine("Points: -"));
        lines.add(contentLine("Cost:"));
        lines.add(contentLine("-"));

        while (lines.size() < 10) {
            lines.add(contentLine(""));
        }

        lines.add("+----------------------+");
        return lines;
    }

    private String contentLine(String content) {
        String trimmed = content.length() > 20 ? content.substring(0, 20) : content;
        StringBuilder sb = new StringBuilder("| ").append(trimmed);
        while (sb.length() < 23) {
            sb.append(' ');
        }
        sb.append("|");
        return sb.toString();
    }

    private String formatNoble(NobleTile noble) {
        StringBuilder req = new StringBuilder();
        for (Map.Entry<GemColor, Integer> e : noble.getRequirement().asMap().entrySet()) {
            req.append(shortColor(e.getKey())).append(":").append(e.getValue()).append(" ");
        }
        return "P:" + noble.getPrestigePoints() + " Req{" + req.toString().trim() + "}";
    }

    private void printBank(GameState state) {
        System.out.println();
        System.out.print("Bank tokens: ");
        for (GemColor color : GemColor.values()) {
            System.out.print(shortColor(color) + "=" + state.getBank().getTokenCount(color) + " ");
        }
        System.out.println();
    }

    private void printPlayers(GameState state) {
        System.out.println();
        System.out.println("Players:");
        for (Player player : state.getPlayers()) {
            System.out.print("- " + player.getName()
                    + " | prestige=" + player.getPrestigePoints()
                    + " | tokens=");
            for (GemColor color : GemColor.values()) {
                System.out.print(shortColor(color) + ":" + player.getTokenCount(color) + " ");
            }
            System.out.print("| bonuses=");
            for (GemColor color : GemColor.values()) {
                if (color == GemColor.GOLD) {
                    continue;
                }
                System.out.print(shortColor(color) + ":" + player.getBonusCount(color) + " ");
            }
            System.out.println("| reserved=" + player.getReservedCards().size());
        }
    }

    private String shortColor(GemColor color) {
        return switch (color) {
            case WHITE -> "W";
            case BLUE -> "B";
            case GREEN -> "G";
            case RED -> "R";
            case BLACK -> "K";
            case GOLD -> "Y";
        };
    }

    private String fullColor(GemColor color) {
        return switch (color) {
            case WHITE -> "White";
            case BLUE -> "Blue";
            case GREEN -> "Green";
            case RED -> "Red";
            case BLACK -> "Black";
            case GOLD -> "Gold";
        };
    }
}
