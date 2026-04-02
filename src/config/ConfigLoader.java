package config;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads and validates Config values from a properties file.
 */
public final class ConfigLoader {

    /**
     * Reads, validates, and resolves configuration values from the supplied
     * properties file.
     *
     * @param configPath path to the configuration file
     * @return the validated configuration object
     * @throws IOException if the file cannot be read
     */
    public Config load(Path configPath) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new BufferedInputStream(
                Files.newInputStream(configPath)
        )) {
            p.load(in);
        }

        Path baseDir = configPath.toAbsolutePath().getParent();

        int pointsToWin = requireInt(p, "pointsToWin", 1, 100);
        int maxTokensPerPlayer = requireInt(p, "maxTokensPerPlayer", 1, 30);
        int maxReservedCards = requireInt(p, "maxReservedCards", 0, 10);
        int maxNoblesPerTurn = requireInt(p, "maxNoblesPerTurn", 1, 3);

        int minPlayers = requireInt(p, "minPlayers", 2, 4);
        int maxPlayers = requireInt(p, "maxPlayers", 2, 4);
        if (minPlayers > maxPlayers) {
            throw new IllegalArgumentException(
                    "minPlayers cannot exceed maxPlayers"
            );
        }

        int numLevels = requireInt(p, "numLevels", 3, 3);
        int openCardsPerLevel = requireInt(p, "openCardsPerLevel", 1, 10);

        int nobles2 = requireInt(p, "noblesCount.2p", 0, 10);
        int nobles3 = requireInt(p, "noblesCount.3p", 0, 10);
        int nobles4 = requireInt(p, "noblesCount.4p", 0, 10);

        int bank2 = requireInt(p, "bank.normal.2p", 0, 10);
        int bank3 = requireInt(p, "bank.normal.3p", 0, 10);
        int bank4 = requireInt(p, "bank.normal.4p", 0, 10);
        int bankGold = requireInt(p, "bank.gold", 0, 10);

        int takeDiff = requireInt(p, "takeDifferent.count", 1, 5);
        int takeSame = requireInt(p, "takeSame.count", 1, bank4);
        int takeSameMin = requireInt(p, "takeSame.minRemainingInBank", 0, 10);

        int reserveGoldBonus = requireInt(p, "reserve.goldBonus", 0, 1);

        Path level1 = requirePath(p, "cards.level1", baseDir);
        Path level2 = requirePath(p, "cards.level2", baseDir);
        Path level3 = requirePath(p, "cards.level3", baseDir);
        Path nobles = requirePath(p, "nobles", baseDir);

        Path cardImageDir = requirePath(p, "cardImageDir", baseDir);
        Path tokenImageDir = requirePath(p, "tokenImageDir", baseDir);
        Path nobleImageDir = requirePath(p, "nobleImageDir", baseDir);

        return new Config(
                pointsToWin,
                maxTokensPerPlayer,
                maxReservedCards,
                maxNoblesPerTurn,
                minPlayers,
                maxPlayers,
                numLevels,
                openCardsPerLevel,
                nobles2,
                nobles3,
                nobles4,
                bank2,
                bank3,
                bank4,
                bankGold,
                takeDiff,
                takeSame,
                takeSameMin,
                reserveGoldBonus,
                level1,
                level2,
                level3,
                nobles,
                cardImageDir,
                tokenImageDir,
                nobleImageDir
        );
    }

    /**
     * Returns a required property value.
     *
     * @param p loaded properties
     * @param key property key to read
     * @return the trimmed property value
     * @throws IllegalArgumentException if the key is missing or blank
     */
    private String require(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing config key: " + key);
        }
        return v.trim();
    }

    /**
     * Returns a required integer property within an allowed range.
     *
     * @param p loaded properties
     * @param key property key to read
     * @param min inclusive minimum value
     * @param max inclusive maximum value
     * @return the parsed integer value
     * @throws IllegalArgumentException if the key is missing, non-numeric, or
     *         outside the allowed range
     */
    private int requireInt(Properties p, String key, int min, int max) {
        String raw = require(p, key);
        try {
            int val = Integer.parseInt(raw);
            if (val < min || val > max) {
                throw new IllegalArgumentException(
                        key + " out of range: " + val
                );
            }
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    key + " must be an integer, got: " + raw
            );
        }
    }

    /**
     * Returns a required path property and resolves relative paths against the
     * configuration file directory.
     *
     * @param p loaded properties
     * @param key property key to read
     * @param baseDir directory used to resolve relative paths
     * @return the normalized absolute or base-relative path
     */
    private Path requirePath(Properties p, String key, Path baseDir) {
        Path path = Path.of(require(p, key));
        if (!path.isAbsolute()) {
            path = baseDir.resolve(path);
        }
        return path.normalize();
    }
}
