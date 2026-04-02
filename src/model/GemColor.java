package model;

/**
 * Enumerates the token colors used by the game, including gold as
 * the wildcard color.
 */
public enum GemColor {
    WHITE,
    BLUE,
    GREEN,
    RED,
    BLACK,
    GOLD;

    /**
     * Indicates whether this color is the wildcard token.
     *
     * @return true when the color is gold
     */
    public boolean isWildCard() {
        return this == GOLD;
    }
}
