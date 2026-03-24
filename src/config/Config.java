package config;

import java.nio.file.Path;

/*
 * Immutable configuration data object(final).
 *
 * This class stores ALL validated configuration values loaded from config.properties.
 * - All fields are final (cannot change after construction)
 * - Values are retrieved via getters
 *
 * ConfigLoader is responsible for validating ranges and resolving relative paths
 * before creating this Config object.
 */

public final class Config {
    //20 getters for 26variables

    // ---------- Core game rules ----------
    private final int pointsToWin;
    private final int maxTokensPerPlayer;
    private final int maxReservedCards;
    private final int maxNoblesPerTurn;

    // ---------- Allowed player counts ----------
    private final int minPlayers;
    private final int maxPlayers;

    // ---------- Board setup ----------
    private final int numLevels;
    private final int openCardsPerLevel;

    // ---------- Nobles count by player count ---------
    private final int noblesCount2p;
    private final int noblesCount3p;
    private final int noblesCount4p;

    // ---------- Bank token counts by player count ----------
    private final int bankNormal2p;
    private final int bankNormal3p;
    private final int bankNormal4p;
    private final int bankGold;

    // ---------- Token-taking rules ----------
    private final int takeDifferentCount;
    private final int takeSameCount;
    private final int takeSameMinRemainingInBank;

    // ---------- Reserve rules ----------
    private final int reserveGoldBonus;

    // ---------- Data file paths ----------
    private final Path level1Path;
    private final Path level2Path;
    private final Path level3Path;
    private final Path noblesPath;

    // ---------- Asset directories ----------
    private final Path cardImageDir;
    private final Path tokenImageDir;
    private final Path nobleImageDir;

    /*
     * Constructs a Config object with all required values.
     * (All validation should be done in ConfigLoader before calling this.)
     */
    public Config(
            int pointsToWin,
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
            Path level1Path,
            Path level2Path,
            Path level3Path,
            Path noblesPath,
            Path cardImageDir,
            Path tokenImageDir,
            Path nobleImageDir
    ) {
        this.pointsToWin = pointsToWin;
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
        this.level1Path = level1Path;
        this.level2Path = level2Path;
        this.level3Path = level3Path;
        this.noblesPath = noblesPath;
        this.cardImageDir = cardImageDir;
        this.tokenImageDir = tokenImageDir;
        this.nobleImageDir = nobleImageDir;
    }

    // ---------- Simple getters ----------
    public int getpointsToWin() { return pointsToWin; }
    public int getMaxTokensPerPlayer() { return maxTokensPerPlayer; }
    public int getMaxReservedCards() { return maxReservedCards; }
    public int getMaxNoblesPerTurn() { return maxNoblesPerTurn; }
    public int getMinPlayer() { return minPlayers; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayer() { return maxPlayers; }
    public int getMaxPlayers() { return maxPlayers; }

    public int getNumLevels() { return numLevels; }
    public int getOpenCardsPerLevel() { return openCardsPerLevel; }

    public int getTakeDifferentCount() { return takeDifferentCount; }
    public int getTakeSameCount() { return takeSameCount; }
    public int getTakeSameMinRemainingInBank() { return takeSameMinRemainingInBank; }

    public int getReserveGoldBonus() { return reserveGoldBonus; }
    public Path getNoblesPath() { return noblesPath; }

    public Path getCardImageDir() { return cardImageDir; }
    public Path getTokenImageDir() { return tokenImageDir; } 
    public Path getNobleImageDir() { return nobleImageDir; }
    
    /*
     * Returns how many nobles should be placed on the board for a given player count.
     */
    public int getNoblesCount(int playerCount) {
        return switch (playerCount) {
            case 2 -> noblesCount2p;
            case 3 -> noblesCount3p;
            case 4 -> noblesCount4p;
            default -> throw new IllegalArgumentException("Unsupported player count: " + playerCount);
        };
    }

    /*
     * Returns how many nobles should be placed on the board for a given player count.
     */
    public int getInitialNormalGemCount(int playerCount) {
        return switch (playerCount) {
            case 2 -> bankNormal2p;
            case 3 -> bankNormal3p;
            case 4 -> bankNormal4p;
            default -> throw new IllegalArgumentException("Unsupported player count: " + playerCount);
        };
    }

    /*
     * Returns how many nobles should be placed on the board for a given player count.
     */
    public int getInitialGoldGemCount(int playerCount) {
        return bankGold;
    }

    /*
     * Returns how many nobles should be placed on the board for a given player count.
     */
    public Path getCardsPath(int level) {
        return switch (level) {
            case 1 -> level1Path;
            case 2 -> level2Path;
            case 3 -> level3Path;
            default -> throw new IllegalArgumentException("Unsupported level: " + level);
        };
    }
    
}
