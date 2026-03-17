package io;

import model.Cost;
import model.DevelopmentCard;
import model.GemColor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CardLoader {

    public Map<Integer, List<DevelopmentCard>> load(Path dataDir) throws IOException {
        Map<Integer, List<DevelopmentCard>> byTier = new HashMap<>();
        byTier.put(1, new ArrayList<>());
        byTier.put(2, new ArrayList<>());
        byTier.put(3, new ArrayList<>());

        for (int level = 1; level <= 3; level++) {
            Path csvPath = dataDir.resolve("level" + level + ".csv");
            List<String> lines = Files.readAllLines(csvPath);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Card CSV is empty: " + csvPath);
            }

            // Skip header
            for (int i = 1; i < lines.size(); i++) {
                String raw = lines.get(i).trim();
                if (raw.isEmpty()) {
                    continue;
                }
                String[] cols = raw.split(",");
                if (cols.length < 9) {
                    throw new IllegalArgumentException("Invalid card row at line " + (i + 1) + ": " + raw);
                }

                int id = i; // Use row number as id
                int csvLevel = parseInt(cols[1], "Level", i + 1);
                if (csvLevel != level) {
                    throw new IllegalArgumentException("Level mismatch at line " + (i + 1) + ": expected " + level + ", got " + csvLevel);
                }
                GemColor bonusColor = parseColor(cols[2]);
                int pv = parseInt(cols[3], "PV", i + 1);

                Cost cost = new Cost();
                int black = parseInt(cols[4], "Black", i + 1);
                int blue = parseInt(cols[5], "Blue", i + 1);
                int green = parseInt(cols[6], "Green", i + 1);
                int red = parseInt(cols[7], "Red", i + 1);
                int white = parseInt(cols[8], "White", i + 1);

                cost.set(GemColor.BLACK, black);
                cost.set(GemColor.BLUE, blue);
                cost.set(GemColor.GREEN, green);
                cost.set(GemColor.RED, red);
                cost.set(GemColor.WHITE, white);

                DevelopmentCard card = new DevelopmentCard(id, level, pv, bonusColor, cost);
                byTier.get(level).add(card);
            }
        }

        // Randomize each tier deck for gameplay.
        Collections.shuffle(byTier.get(1));
        Collections.shuffle(byTier.get(2));
        Collections.shuffle(byTier.get(3));

        return byTier;
    }

    private int parseTier(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing tier value");
        }
        // Supports format like "1", "2", "3", or values where first char is tier.
        char first = value.charAt(0);
        if (!Character.isDigit(first)) {
            throw new IllegalArgumentException("Invalid tier value: " + raw);
        }
        int tier = Character.digit(first, 10);
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("Tier must be 1-3, got: " + raw);
        }
        return tier;
    }

    private GemColor parseColor(String raw) {
        String c = raw.trim().toUpperCase(Locale.ROOT);
        return switch (c) {
            case "WHITE" -> GemColor.WHITE;
            case "BLUE" -> GemColor.BLUE;
            case "GREEN" -> GemColor.GREEN;
            case "RED" -> GemColor.RED;
            case "BLACK" -> GemColor.BLACK;
            default -> throw new IllegalArgumentException("Unknown bonus color: " + raw);
        };
    }

    private int parseInt(String raw, String column, int lineNo) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer in " + column + " at line " + lineNo + ": " + raw);
        }
    }
}
