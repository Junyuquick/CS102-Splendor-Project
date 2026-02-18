package splendor.config;

import java.nio.file.Path;

public final class Config {
    private final int winningPrestige;
    private final int maxTokensPerPlayer;
    private final int maxReservedCards;
    private final int maxNoblesPerTurn;
    private final int minPlayers;
    private final int maxPlayers;

    private final int numLevels;
    private final int openCardsPerLevel;

    private final int noblesCount2p;
    private final int noblesCount3p;
    private final int noblesCount4p;

    private final int bankNormal2p;
    private final int bankNormal3p;
    private final int bankNormal4p;
    private final int bankGold;

    private final int takeDifferentCount;
    private final int takeSameCount;
    private final int takeSameMinRemainingInBank;

    private final int reserveGoldBonus;
    private final boolean reserveFromDeckEnabled;
    private final boolean reserveFromFaceUpEnabled;

    private final Path level1Path;
    private final Path level2Path;
    private final Path level3Path;
    private final Path noblesPath;

    // Optional if you have images in your app:
    private final Path cardImageDir;
    private final Path tokenImageDir;
    private final Path nobleImageDir;

    public Config(
            int winningPrestige,
            int maxTokensPerPlayer,
            int maxReservedCards,
            int maxNoblesPerTurn,
            int minPlayers,
            int maxPlayers,
            int numLevels,
            int openCardsPerLevel,
            int noblesCount2p,
            int noblesCount3p,
            int noblesCount4p,
            int bankNormal2p,
            int bankNormal3p,
            int bankNormal4p,
            int bankGold,
            int takeDifferentCount,
            int takeSameCount,
            int takeSameMinRemainingInBank,
            int reserveGoldBonus,
            boolean reserveFromDeckEnabled,
            boolean reserveFromFaceUpEnabled,
            Path level1Path,
            Path level2Path,
            Path level3Path,
            Path noblesPath,
            Path cardImageDir,
            Path tokenImageDir,
            Path nobleImageDir
    ) {
        this.winningPrestige = winningPrestige;
        this.maxTokensPerPlayer = maxTokensPerPlayer;
        this.maxReservedCards = maxReservedCards;
        this.maxNoblesPerTurn = maxNoblesPerTurn;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.numLevels = numLevels;
        this.openCardsPerLevel = openCardsPerLevel;
        this.noblesCount2p = noblesCount2p;
        this.noblesCount3p = noblesCount3p;
        this.noblesCount4p = noblesCount4p;
        this.bankNormal2p = bankNormal2p;
        this.bankNormal3p = bankNormal3p;
        this.bankNormal4p = bankNormal4p;
        this.bankGold = bankGold;
        this.takeDifferentCount = takeDifferentCount;
        this.takeSameCount = takeSameCount;
        this.takeSameMinRemainingInBank = takeSameMinRemainingInBank;
        this.reserveGoldBonus = reserveGoldBonus;
        this.reserveFromDeckEnabled = reserveFromDeckEnabled;
        this.reserveFromFaceUpEnabled = reserveFromFaceUpEnabled;
        this.level1Path = level1Path;
        this.level2Path = level2Path;
        this.level3Path = level3Path;
        this.noblesPath = noblesPath;
        this.cardImageDir = cardImageDir;
        this.tokenImageDir = tokenImageDir;
        this.nobleImageDir = nobleImageDir;
    }

    // ---- getters (examples) ----
    public int getWinningPrestige() { return winningPrestige; }
    public int getMaxTokensPerPlayer() { return maxTokensPerPlayer; }
    public int getOpenCardsPerLevel() { return openCardsPerLevel; }
    public int getNumLevels() { return numLevels; }

    public Path getLevel1Path() { return level1Path; }
    public Path getLevel2Path() { return level2Path; }
    public Path getLevel3Path() { return level3Path; }
    public Path getNoblesPath() { return noblesPath; }

    public Path getCardImageDir() { return cardImageDir; }
    public Path getTokenImageDir() { return tokenImageDir; }
    public Path getNobleImageDir() { return nobleImageDir; }

    // ---- derived helpers (avoid magic logic in engine) ----
    public int getInitialNormalGemCount(int playerCount) {
        return switch (playerCount) {
            case 2 -> bankNormal2p;
            case 3 -> bankNormal3p;
            case 4 -> bankNormal4p;
            default -> throw new IllegalArgumentException("Unsupported player count: " + playerCount);
        };
    }

    public int getInitialGoldGemCount(int playerCount) {
        return bankGold;
    }

    public int getNoblesCount(int playerCount) {
        return switch (playerCount) {
            case 2 -> noblesCount2p;
            case 3 -> noblesCount3p;
            case 4 -> noblesCount4p;
            default -> throw new IllegalArgumentException("Unsupported player count: " + playerCount);
        };
    }

    public Path getCardsPath(int level) {
        return switch (level) {
            case 1 -> level1Path;
            case 2 -> level2Path;
            case 3 -> level3Path;
            default -> throw new IllegalArgumentException("Unsupported level: " + level);
        };
    }
}