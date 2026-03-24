package ui.swing;

import config.Config;
import model.DevelopmentCard;
import model.GemColor;
import model.Player;

import java.util.EnumMap;
import java.util.Map;

final class SwingPlayerSummaryFormatter {
    private SwingPlayerSummaryFormatter() {
    }

    static String buildRichHtml(Player player, Config config) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:sans-serif;font-size:12px;color:#E8ECF1;'>");
        html.append("<b>Prestige:</b> ").append(player.getPrestigePoints()).append("<br/>");
        html.append("<b>Tokens + Bonuses</b><br/>");

        GemColor[] order = {GemColor.RED, GemColor.BLUE, GemColor.GREEN, GemColor.WHITE, GemColor.BLACK, GemColor.GOLD};
        for (GemColor color : order) {
            int tokens = player.getTokenCount(color);
            int bonus = color == GemColor.GOLD ? 0 : player.getBonusCount(color);
            html.append(color.name().toLowerCase()).append(": ").append(tokens);
            if (bonus > 0) {
                html.append(" <span style='color:")
                        .append(hexColor(color))
                        .append(";font-weight:bold;'>+ ")
                        .append(bonus)
                        .append("</span>");
            }
            html.append("<br/>");
        }

        html.append("<b>Purchased:</b> ").append(player.getPurchasedCards().size())
                .append(" | <b>Nobles:</b> ").append(player.getNobles().size())
                .append(" | <b>Reserved:</b> ").append(player.getReservedCards().size())
                .append("<br/>");
        html.append("<b>Token cap:</b> ").append(player.getTotalTokens())
                .append("/").append(config.getMaxTokensPerPlayer()).append("<br/>");
        html.append("<b>Reserved cards:</b> ").append(compactReserved(player));
        html.append("</body></html>");
        return html.toString();
    }

    static String buildCompactText(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("Prestige: ").append(player.getPrestigePoints()).append("\n");
        sb.append("Tokens: ").append(player.getTokens()).append("\n");
        sb.append("Bonuses: ").append(getBonusCounts(player)).append("\n");
        sb.append("Purchased: ").append(player.getPurchasedCards().size()).append("\n");
        sb.append("Reserved: ").append(player.getReservedCards().size()).append("\n");
        sb.append("Nobles: ").append(player.getNobles().size()).append("\n");
        return sb.toString();
    }

    private static String compactReserved(Player player) {
        if (player.getReservedCards().isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (DevelopmentCard card : player.getReservedCards()) {
            if (index > 1) {
                sb.append(", ");
            }
            sb.append("r").append(index)
                    .append("(P").append(card.getPrestigePoints())
                    .append(" ").append(card.getBonusColor().name().toLowerCase()).append(")");
            index++;
        }
        return sb.toString();
    }

    private static Map<GemColor, Integer> getBonusCounts(Player player) {
        Map<GemColor, Integer> bonus = new EnumMap<>(GemColor.class);
        for (GemColor color : GemColor.values()) {
            if (color != GemColor.GOLD) {
                bonus.put(color, player.getBonusCount(color));
            }
        }
        return bonus;
    }

    private static String hexColor(GemColor color) {
        return switch (color) {
            case WHITE -> "#8A8A8A";
            case BLUE -> "#1E5BB8";
            case GREEN -> "#1E8C3A";
            case RED -> "#B3261E";
            case BLACK -> "#111111";
            case GOLD -> "#B88700";
        };
    }
}
