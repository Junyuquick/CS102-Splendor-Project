package config;

import java.nio.file.Path;

/**
 * immutable set of validated config values for games of Splendor.
 * Singular instances are created by the ConfigLoader file after the raw properties file
 * has been parsed, validated, and any relative paths have been resolved.
 */
public final class Config {
    private final int pointsToWin;
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
    private final Path level1Path;
    private final Path level2Path;
    private final Path level3Path;
    private final Path noblesPath;
    private final Path cardImageDir;
    private final Path tokenImageDir;
    private final Path nobleImageDir;

    /**
     * Creates a configuration object from already validated values.
     * @param pointsToWin prestige points required to conclude game
     * @param maxTokensPerPlayer maximum number of tokens a player can hold at once
     * @param maxReservedCards maximum number of cards a player can reserve
     * @param maxNoblesPerTurn maximum number of nobles that can be assigned after one turn
     * @param minPlayers minimum player count
     * @param maxPlayers maximum player count
     * @param numLevels number of development card tiers in use
     * @param openCardsPerLevel number of face up cards for each tier
     * @param noblesCount2p noble count for two player game
     * @param noblesCount3p noble count for three player game
     * @param noblesCount4p noble count for four player game
     * @param bankNormal2p starting token count (excluding gold) for two player game
     * @param bankNormal3p starting token count for (excluding gold) three player game
     * @param bankNormal4p starting token count for (excluding gold) four player game
     * @param bankGold starting gold token count
     * @param takeDifferentCount number of differently coloured tokens that can be taken in one move
     * @param takeSameCount number of identical tokens that can be taken in one move
     * @param takeSameMinRemainingInBank minimum number of tokens that must remain after taking two of one color
     * @param reserveGoldBonus gold token bonus awarded when reserving a card
     * @param level1Path path to level 1 card data
     * @param level2Path path to level 2 card data
     * @param level3Path path to level 3 card data
     * @param noblesPath path to noble data
     * @param cardImageDir directory containing development card images
     * @param tokenImageDir directory containing token images
     * @param nobleImageDir directory containing noble images
     */
    public Config (int pointsToWin, int maxTokensPerPlayer, int maxReservedCards, int maxNoblesPerTurn,
                    int minPlayers, int maxPlayers, int numLevels, int openCardsPerLevel, int noblesCount2p,
                    int noblesCount3p, int noblesCount4p, int bankNormal2p, int bankNormal3p, int bankNormal4p,
                    int bankGold, int takeDifferentCount, int takeSameCount, int takeSameMinRemainingInBank,
                    int reserveGoldBonus, Path level1Path, Path level2Path, Path level3Path, Path noblesPath,
                    Path cardImageDir, Path tokenImageDir, Path nobleImageDir) {
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

    /**
     * Returns prestige point count that triggers the end of the game.
     *
     * @return winning score threshold
     */
    public int getPointsToWin() {
        return pointsToWin;
    }

/**
     * Returns prestige point count that triggers the end of the game.
     *
     * for compatibility with existing callers
     * 
     * @return winning score threshold
     */
    public int getpointsToWin() {
        return getPointsToWin();
    }

    /**
     * Returns maximum number of tokens a player may hold.
     *
     * @return player token limit
     */
    public int getMaxTokensPerPlayer() {
        return maxTokensPerPlayer;
    }

    /**
     * Returns maximum number of reserved cards per player.
     *
     * @return reserve limit
     */
    public int getMaxReservedCards() {
        return maxReservedCards;
    }

    /**
     * Returns maximum number of nobles that can be assigned after one turn.
     *
     * @return noble assignment limit
     */
    public int getMaxNoblesPerTurn() {
        return maxNoblesPerTurn; 
    }

    /**
     * Returns minimum supported player count.
     *
     * @return minimum player count
     */
    public int getMinPlayers() {
        return minPlayers; 
    }

    /**
     * Returns maximum supported player count.
     *
     * @return maximum player count
     */
    public int getMaxPlayers() {
        return maxPlayers; 
    }

    /**
     * Returns number of development card tiers used by the game.
     *
     * @return tier count
     */
    public int getNumLevels() { 
        return numLevels; 
    }

    /**
     * Returns number of face up cards shown for each tier.
     *
     * @return number of open cards per tier
     */
    public int getOpenCardsPerLevel() {
        return openCardsPerLevel; 
    }

    /**
     * Returns how many differently colored tokens can be taken in one move.
     *
     * @return take-different-token count
     */
    public int getTakeDifferentCount() { 
        return takeDifferentCount; 
    }

    /**
     * Returns how many identical tokens can be taken in one move.
     *
     * @return take-same-token count
     */
    public int getTakeSameCount() {
        return takeSameCount;
    }

    /**
     * Returns minimum bank balance required after taking two of one color.
     *
     * @return minimum remaining tokens in bank
     */
    public int getTakeSameMinRemainingInBank() {
        return takeSameMinRemainingInBank; 
    }

    /**
     * Returns gold-token bonus awarded when a card is reserved.
     *
     * @return reserve gold bonus
     */
    public int getReserveGoldBonus() {
        return reserveGoldBonus; 
    }

    /**
     * Returns path to noble data file.
     *
     * @return nobles CSV path
     */
    public Path getNoblesPath() {
        return noblesPath;
    }

    /**
     * Returns directory containing card artwork.
     *
     * @return card image directory
     */
    public Path getCardImageDir() {
        return cardImageDir;
    }

    /**
     * Returns directory containing token artwork.
     *
     * @return token image directory
     */
    public Path getTokenImageDir() { 
        return tokenImageDir; 
    }

    /**
     * Returns directory containing noble artwork.
     *
     * @return noble image directory
     */
    public Path getNobleImageDir() {
        return nobleImageDir; 
    }

    /**
     * Returns the number of nobles that should be placed on the board for a particular game size.
     *
     * @param playerCount active player count
     * @return noble count for that game size
     * @throws IllegalArgumentException if playerCount is unsupported
     */
    public int getNoblesCount(int playerCount) {
        return switch (playerCount) {
            case 2 -> noblesCount2p;
            case 3 -> noblesCount3p;
            case 4 -> noblesCount4p;
            default -> throw new IllegalArgumentException(
                    "Unsupported player count: " + playerCount
            );
        };
    }

    /**
     * Returns starting count for each non gold token color for a particular game size.
     *
     * @param playerCount active player count
     * @return starting supply for each normal token color
     * @throws IllegalArgumentException if playerCount is unsupported
     */
    public int getInitialNormalGemCount(int playerCount) {
        return switch (playerCount) {
            case 2 -> bankNormal2p;
            case 3 -> bankNormal3p;
            case 4 -> bankNormal4p;
            default -> throw new IllegalArgumentException(
                    "Unsupported player count: " + playerCount
            );
        };
    }

    /**
     * Returns the starting number of gold tokens.
     *
     * @param playerCount active player count
     * @return starting gold token supply
     */
    public int getInitialGoldGemCount(int playerCount) {
        return bankGold;
    }

    /**
     * Returns card data file for requested tier.
     *
     * @param level development card level
     * @return path to CSV file for that tier
     * @throws IllegalArgumentException if level is unsupported
     */
    public Path getCardsPath(int level) {
        return switch (level) {
            case 1 -> level1Path;
            case 2 -> level2Path;
            case 3 -> level3Path;
            default -> throw new IllegalArgumentException(
                    "Unsupported level: " + level
            );
        };
    }
}
