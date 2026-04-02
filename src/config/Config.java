package config;

import java.nio.file.Path;

/**
 * Immutable bundle of validated configuration values for a Splendor game.
 *
 * Instances are created by ConfigLoader after the raw properties file has been
 * parsed, validated, and any relative paths have been resolved.
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
     *
     * @param pointsToWin prestige points required to trigger the endgame
     * @param maxTokensPerPlayer maximum number of tokens a player may hold
     * @param maxReservedCards maximum number of reserved cards per player
     * @param maxNoblesPerTurn maximum number of nobles assignable after one
     *        turn
     * @param minPlayers minimum supported player count
     * @param maxPlayers maximum supported player count
     * @param numLevels number of development-card tiers in use
     * @param openCardsPerLevel number of face-up cards shown for each tier
     * @param noblesCount2p noble count for a two-player game
     * @param noblesCount3p noble count for a three-player game
     * @param noblesCount4p noble count for a four-player game
     * @param bankNormal2p starting non-gold token count for a two-player game
     * @param bankNormal3p starting non-gold token count for a three-player
     *        game
     * @param bankNormal4p starting non-gold token count for a four-player game
     * @param bankGold starting gold token count
     * @param takeDifferentCount number of differently colored tokens allowed in
     *        one take move
     * @param takeSameCount number of identical tokens allowed in one take move
     * @param takeSameMinRemainingInBank minimum number of tokens that must
     *        remain after taking two of one color
     * @param reserveGoldBonus gold-token bonus awarded when reserving a card
     * @param level1Path path to the level-1 card data
     * @param level2Path path to the level-2 card data
     * @param level3Path path to the level-3 card data
     * @param noblesPath path to the noble data
     * @param cardImageDir directory containing development-card images
     * @param tokenImageDir directory containing token images
     * @param nobleImageDir directory containing noble images
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

    /**
     * Returns the prestige-point threshold that triggers the final round.
     *
     * @return the winning score threshold
     */
    public int getPointsToWin() {
        return pointsToWin;
    }

    /**
     * Returns the prestige-point threshold that triggers the final round.
     *
     * Compatibility alias kept for existing callers.
     * 
     * @return the winning score threshold
     */
    public int getpointsToWin() {
        return getPointsToWin();
    }

    /**
     * Returns the maximum number of tokens a player may hold.
     *
     * @return the player token limit
     */
    public int getMaxTokensPerPlayer() {
        return maxTokensPerPlayer;
    }

    /**
     * Returns the maximum number of reserved cards per player.
     *
     * @return the reserve limit
     */
    public int getMaxReservedCards() {
        return maxReservedCards;
    }

    /**
     * Returns the maximum number of nobles that can be assigned after one
     * turn.
     *
     * @return the noble-assignment limit
     */
    public int getMaxNoblesPerTurn() {
        return maxNoblesPerTurn;
    }

    /**
     * Returns the minimum supported player count.
     *
     * @return the minimum player count
     */
    public int getMinPlayer() {
        return minPlayers;
    }

    /**
     * Returns the minimum supported player count.
     *
     * @return the minimum player count
     */
    public int getMinPlayers() {
        return minPlayers;
    }

    /**
     * Returns the maximum supported player count.
     *
     * @return the maximum player count
     */
    public int getMaxPlayer() {
        return maxPlayers;
    }

    /**
     * Returns the maximum supported player count.
     *
     * @return the maximum player count
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Returns the number of development-card tiers used by the game.
     *
     * @return the tier count
     */
    public int getNumLevels() {
        return numLevels;
    }

    /**
     * Returns the number of face-up cards shown for each tier.
     *
     * @return the number of open cards per tier
     */
    public int getOpenCardsPerLevel() {
        return openCardsPerLevel;
    }

    /**
     * Returns how many differently colored tokens can be taken in one move.
     *
     * @return the take-different token count
     */
    public int getTakeDifferentCount() {
        return takeDifferentCount;
    }

    /**
     * Returns how many identical tokens can be taken in one move.
     *
     * @return the take-same token count
     */
    public int getTakeSameCount() {
        return takeSameCount;
    }

    /**
     * Returns the minimum bank balance required after taking two of one color.
     *
     * @return the minimum remaining tokens in the bank
     */
    public int getTakeSameMinRemainingInBank() {
        return takeSameMinRemainingInBank;
    }

    /**
     * Returns the gold-token bonus awarded when a card is reserved.
     *
     * @return the reserve gold bonus
     */
    public int getReserveGoldBonus() {
        return reserveGoldBonus;
    }

    /**
     * Returns the path to the noble data file.
     *
     * @return the nobles CSV path
     */
    public Path getNoblesPath() {
        return noblesPath;
    }

    /**
     * Returns the directory containing card artwork.
     *
     * @return the card image directory
     */
    public Path getCardImageDir() {
        return cardImageDir;
    }

    /**
     * Returns the directory containing token artwork.
     *
     * @return the token image directory
     */
    public Path getTokenImageDir() {
        return tokenImageDir;
    }

    /**
     * Returns the directory containing noble artwork.
     *
     * @return the noble image directory
     */
    public Path getNobleImageDir() {
        return nobleImageDir;
    }

    /**
     * Returns how many nobles should be placed on the board for a game size.
     *
     * @param playerCount active player count
     * @return the noble count for that game size
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
     * Returns the starting count for each non-gold token color for a game
     * size.
     *
     * @param playerCount active player count
     * @return the starting supply for each normal token color
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
     * @param playerCount active player count included for API symmetry with
     *        normal tokens
     * @return the starting gold-token supply
     */
    public int getInitialGoldGemCount(int playerCount) {
        return bankGold;
    }

    /**
     * Returns the card-data file for the requested tier.
     *
     * @param level development-card level
     * @return the path to the CSV file for that tier
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
