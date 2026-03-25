package io;

import model.Cost;
import model.GemColor;
import model.NobleTile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads noble-tile data from CSV input.
 */
public class NobleLoader {
    /**
     * Loads nobles from a CSV file.
     *
     * @param csvPath path to the noble CSV file
     * @return the nobles parsed from that file
     * @throws IOException if the file cannot be read
     */
    public List<NobleTile> load(Path csvPath) throws IOException {
        List<String> lines = Files.readAllLines(csvPath);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Noble CSV is empty: " + csvPath);
        }

        List<NobleTile> nobles = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String raw = lines.get(i).trim();
            if (raw.isEmpty()) {
                continue;
            }

            String[] cols = raw.split(",");
            if (cols.length < 7) {
                throw new IllegalArgumentException("Invalid noble row at line " + (i + 1) + ": " + raw);
            }

            int id = parseCardId(cols[0]);
            int points = Integer.parseInt(cols[1].trim());

            Cost cost = new Cost();
            cost.set(GemColor.WHITE, Integer.parseInt(cols[2].trim()));
            cost.set(GemColor.BLUE, Integer.parseInt(cols[3].trim()));
            cost.set(GemColor.GREEN, Integer.parseInt(cols[4].trim()));
            cost.set(GemColor.RED, Integer.parseInt(cols[5].trim()));
            cost.set(GemColor.BLACK, Integer.parseInt(cols[6].trim()));

            nobles.add(new NobleTile(id, points, cost));
        }

        return nobles;
    }

    private int parseCardId(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("card_")) {
            trimmed = trimmed.substring("card_".length());
        }
        return Integer.parseInt(trimmed);
    }
}
