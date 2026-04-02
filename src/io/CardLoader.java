package io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import model.Cost;
import model.DevelopmentCard;
import model.GemColor;

/**
 * Loads DevelopementCard data from CSV files.
 */
public class CardLoader {
    
    /**
     * Loads and shuffles card decks for all three tiers: level1,level2 and level3 from separate CSV files.
     *
     * @param level1Path path to the level-1 card data
     * @param level2Path path to the level-2 card data
     * @param level3Path path to the level-3 card data
     * @return cards grouped by tier number: level1, 2 and 3 repectively
     * @throws IOException if any CSV file cannot be read
     */

    public Map<Integer, List<DevelopmentCard>> load(Path level1Path, Path level2Path, Path level3Path) throws IOException {
        Map<Integer, List<DevelopmentCard>> byTier = new HashMap<>();
        byTier.put(1, new ArrayList<>());
        byTier.put(2, new ArrayList<>());
        byTier.put(3, new ArrayList<>());

        Map<Integer, Path> csvPaths = Map.of(
                1, level1Path,
                2, level2Path,
                3, level3Path
        );

        for (int level = 1; level <= 3; level++) {
            Path csvPath = csvPaths.get(level);
            List<String> lines = Files.readAllLines(csvPath);
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Card CSV is empty: " + csvPath);
            }

            for (int i = 1; i < lines.size(); i++) {
                String raw = lines.get(i).trim();
                if (raw.isEmpty()) {
                    continue;
                }
                String[] cols = raw.split(",");
                if (cols.length < 9) {
                    throw new IllegalArgumentException("Invalid card row at line " + (i + 1) + ": " + raw);
                }

                int id = i;
                int csvLevel = parseInt(cols[1], "Level", i + 1);
                if (csvLevel != level) {
                    throw new IllegalArgumentException("Level mismatch at line " + (i + 1) + ": expected " + level + ", got " + csvLevel);
                }
                GemColor bonusColor = parseColor(cols[2]);
                int pv = parseInt(cols[3], "PV", i + 1);
                validatePrestigeRange(level, pv, i + 1);

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

        Collections.shuffle(byTier.get(1));
        Collections.shuffle(byTier.get(2));
        Collections.shuffle(byTier.get(3));
        return byTier;
    }

    /**
     * Loads and shuffles card decks using the card data file names inside the directory containing them, using the load method above
     *
     * @param dataDir directory containing level1.csv, level2.csv and level3.csv data files
     * @return cards grouped by tier number
     * @throws IOException if any CSV file cannot be read
     */
    public Map<Integer, List<DevelopmentCard>> load(Path dataDir) throws IOException {
        return load(
                dataDir.resolve("level1.csv"),
                dataDir.resolve("level2.csv"),
                dataDir.resolve("level3.csv")
        );
    }

    /**
     * Converts color in string from data file to Gemcolor class
     */
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

    /**
     * Same as Java.lang parseInt, but specifies the source of error in data file if integer is invalid
     */
    private int parseInt(String raw, String column, int lineNo) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer in " + column + " at line " + lineNo + ": " + raw);
        }
    }

    /**
     * Check if points allocated to a card is within the range for its level
     */
    private void validatePrestigeRange(int level, int prestigePoints, int lineNo) {
        boolean valid = switch (level) {
            case 1 -> prestigePoints >= 0 && prestigePoints <= 1;
            case 2 -> prestigePoints >= 1 && prestigePoints <= 3;
            case 3 -> prestigePoints >= 3 && prestigePoints <= 5;
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid PV for level " + level + " at line " + lineNo + ": " + prestigePoints
            );
        }
    }
}
