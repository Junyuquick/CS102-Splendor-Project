package splendor.config;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigLoader {

    public Config load(Path configPath) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(configPath))) {
            p.load(in);
        }

        // base directory for relative paths
        Path baseDir = configPath.toAbsolutePath().getParent();

        int winningPrestige = requireInt(p, "winningPrestige", 1, 100);
        int maxTokensPerPlayer = requireInt(p, "maxTokensPerPlayer", 1, 30);
        int maxReservedCards = requireInt(p, "maxReservedCards", 0, 10);
        int maxNoblesPerTurn = requireInt(p, "maxNoblesPerTurn", 1, 3);

        int minPlayers = requireInt(p, "minPlayers", 2, 4);
        int maxPlayers = requireInt(p, "maxPlayers", 2, 4);
        if (minPlayers > maxPlayers) {
            throw new IllegalArgumentException("minPlayers cannot exceed maxPlayers");
        }

        int numLevels = requireInt(p, "numLevels", 3, 3); // enforce base-game
        int openCardsPerLevel = requireInt(p, "openCardsPerLevel", 1, 10);

        int nobles2 = requireInt(p, "noblesCount.2p", 0, 10);
        int nobles3 = requireInt(p, "noblesCount.3p", 0, 10);
        int nobles4 = requireInt(p, "noblesCount.4p", 0, 10);

        int bank2 = requireInt(p, "bank.normal.2p", 0, 10);
        int bank3 = requireInt(p, "bank.normal.3p", 0, 10);
        int bank4 = requireInt(p, "bank.normal.4p", 0, 10);
        int bankGold = requireInt(p, "bank.gold", 0, 10);

        int takeDiff = requireInt(p, "takeDifferent.count", 1, 3);
        int takeSame = requireInt(p, "takeSame.count", 1, 2);
        int takeSameMin = requireInt(p, "takeSame.minRemainingInBank", 0, 10);

        int reserveGoldBonus = requireInt(p, "reserve.goldBonus", 0, 1);
        boolean reserveFromDeck = requireBoolean(p, "reserve.fromDeckEnabled");
        boolean reserveFromFaceUp = requireBoolean(p, "reserve.fromFaceUpEnabled");

        Path level1 = requirePath(p, "cards.level1", baseDir);
        Path level2 = requirePath(p, "cards.level2", baseDir);
        Path level3 = requirePath(p, "cards.level3", baseDir);
        Path nobles = requirePath(p, "nobles", baseDir);

        ensureReadable(level1, "cards.level1");
        ensureReadable(level2, "cards.level2");
        ensureReadable(level3, "cards.level3");
        ensureReadable(nobles, "nobles");

        // Optional image directories (if keys missing, set to null or baseDir.resolve("assets/..."))
        Path cardImageDir = optionalPath(p, "cardImageDir", baseDir, null);
        Path tokenImageDir = optionalPath(p, "tokenImageDir", baseDir, null);
        Path nobleImageDir = optionalPath(p, "nobleImageDir", baseDir, null);

        return new Config(
                winningPrestige, maxTokensPerPlayer, maxReservedCards, maxNoblesPerTurn,
                minPlayers, maxPlayers,
                numLevels, openCardsPerLevel,
                nobles2, nobles3, nobles4,
                bank2, bank3, bank4, bankGold,
                takeDiff, takeSame, takeSameMin,
                reserveGoldBonus, reserveFromDeck, reserveFromFaceUp,
                level1, level2, level3, nobles,
                cardImageDir, tokenImageDir, nobleImageDir
        );
    }

    private String require(Properties p, String key) {
        String v = p.getProperty(key);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing config key: " + key);
        return v.trim();
    }

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

    private boolean requireBoolean(Properties p, String key) {
        String raw = require(p, key).toLowerCase();
        if (raw.equals("true")) return true;
        if (raw.equals("false")) return false;
        throw new IllegalArgumentException(key + " must be true/false, got: " + raw);
    }

    private Path requirePath(Properties p, String key, Path baseDir) {
        Path path = Path.of(require(p, key));
        if (!path.isAbsolute()) path = baseDir.resolve(path);
        return path.normalize();
    }

}