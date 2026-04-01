package model;

/**
 * Enums the respective colors of gems, with gold being the wild card.
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
     */
    public boolean isWildCard() {
        return this == GOLD;
    }

}
