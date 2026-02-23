package config;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/*
 * to be instantiated then referenced to call method: load
 * Loads and validates configuration values from config.properties file,
 * then constructs a Config object.
 */
public final class ConfigLoader {

    /*
     * Reads the config file at configPath(relative path), validates required keys and min/max,
     * resolves any relative paths against the config file's directory,
     * and returns a Config object with all the values in config.properties
     */
    public Config load(Path configPath) throws IOException {

        // Load key=value pairs from config file into Properties object(a map)
        Properties p = new Properties();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(configPath))) {
            p.load(in);
        }

        // Base directory of the config file (used to resolve relative file paths)
        Path baseDir = configPath.toAbsolutePath().getParent();

        // ---------- Basic game rules ----------
        int pointsToWin = requireInt(p, "pointsToWin", 1, 100);
        int maxTokensPerPlayer = requireInt(p, "maxTokensPerPlayer", 1, 30);
        int maxReservedCards = requireInt(p, "maxReservedCards", 0, 10);
        int maxNoblesPerTurn = requireInt(p, "maxNoblesPerTurn", 1, 3);
        
        // ---------- Player count ----------
        int minPlayers = requireInt(p, "minPlayers", 2, 4);
        int maxPlayers = requireInt(p, "maxPlayers", 2, 4);
        if (minPlayers > maxPlayers) {
            throw new IllegalArgumentException("minPlayers cannot exceed maxPlayers");
        }

        // ---------- Board setup ----------
        int numLevels = requireInt(p, "numLevels", 3, 3); // enforce base-game
        int openCardsPerLevel = requireInt(p, "openCardsPerLevel", 1, 10);
        
        // ---------- Nobles per player count ----------
        int nobles2 = requireInt(p, "noblesCount.2p", 0, 10);
        int nobles3 = requireInt(p, "noblesCount.3p", 0, 10);
        int nobles4 = requireInt(p, "noblesCount.4p", 0, 10);
        
        // ---------- Bank token counts ----------
        int bank2 = requireInt(p, "bank.normal.2p", 0, 10);
        int bank3 = requireInt(p, "bank.normal.3p", 0, 10);
        int bank4 = requireInt(p, "bank.normal.4p", 0, 10);
        int bankGold = requireInt(p, "bank.gold", 0, 10);
        
        // ---------- Token-taking rules ----------
        int takeDiff = requireInt(p, "takeDifferent.count", 1, 3);
        int takeSame = requireInt(p, "takeSame.count", 1, 2);
        int takeSameMin = requireInt(p, "takeSame.minRemainingInBank", 0, 10);
        
        // ---------- Reserve rules ----------
        int reserveGoldBonus = requireInt(p, "reserve.goldBonus", 0, 1);

        // ---------- Data file paths (resolve relative paths against baseDir) ----------

        Path level1 = requirePath(p, "cards.level1", baseDir);
        Path level2 = requirePath(p, "cards.level2", baseDir);
        Path level3 = requirePath(p, "cards.level3", baseDir);
        Path nobles = requirePath(p, "nobles", baseDir);
        
        // ---------- Asset directories ----------
        Path cardImageDir = requirePath(p, "cardImageDir", baseDir);
        Path tokenImageDir = requirePath(p, "tokenImageDir", baseDir);
        Path nobleImageDir = requirePath(p, "nobleImageDir", baseDir);
        
        // Construct and return Config after all values are validated and resolved
        return new Config(
                pointsToWin, maxTokensPerPlayer, maxReservedCards, maxNoblesPerTurn,
                minPlayers, maxPlayers,
                numLevels, openCardsPerLevel,
                nobles2, nobles3, nobles4,
                bank2, bank3, bank4, bankGold,
                takeDiff, takeSame, takeSameMin,
                reserveGoldBonus,
                level1, level2, level3, nobles,
                cardImageDir, tokenImageDir, nobleImageDir
        );
    }

    /*
     * Retrieves a required config value.
     * Throws exception if missing or blank.
     */
    private String require(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing config key: " + key);
        return v.trim();
    }
    /*
     * Retrieves a required integer config value and enforces a min/max range.
     * Throws exception if missing, not an integer, or out of range.
     */
    private int requireInt(Properties p, String key, int min, int max) {
        String raw = require(p, key);
        try {
            int val = Integer.parseInt(raw);
            if (val < min || val > max) throw new IllegalArgumentException(key + " out of range: " + val);
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer, got: " + raw);
        }
    }

    /*
     * Retrieves a required path config value.
     * If the path is relative, resolve it against baseDir (config file's directory, aka our splendor root directory).
     */
    private Path requirePath(Properties p, String key, Path baseDir) {
        Path path = Path.of(require(p, key));
        if (!path.isAbsolute()) path = baseDir.resolve(path);
        return path.normalize();
    }
}




//SAMPLE OF HOW TO CALL CONFIGLOADER:
        // import config.*;
        // import java.nio.file.Path;
        // import java.io.IOException;
        
        // try {
        //     ConfigLoader loader = new ConfigLoader();
        //     Config config = loader.load(Path.of("config.properties"));
        //     System.out.printf("getMaxTokensPerPlayer: %d", config.getMaxTokensPerPlayer());
        // } catch (IOException e) {
        //     System.out.print("Something went wrong");
        // }





 // temporarily no need the below
    // private boolean requireBoolean(Properties p, String key) {
    //     String raw = require(p, key).toLowerCase();
    //     if (raw.equals("true")) return true;
    //     if (raw.equals("false")) return false;
    //     throw new IllegalArgumentException(key + " must be true/false, got: " + raw);
    // }