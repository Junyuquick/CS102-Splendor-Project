package model;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Defines the token colors used by the game, including gold as
 * the wildcard color.
 */
public final class GemColor implements Serializable {
    public static final GemColor WHITE = new GemColor("WHITE");
    public static final GemColor BLUE = new GemColor("BLUE");
    public static final GemColor GREEN = new GemColor("GREEN");
    public static final GemColor RED = new GemColor("RED");
    public static final GemColor BLACK = new GemColor("BLACK");
    public static final GemColor GOLD = new GemColor("GOLD");

    private static final GemColor[] VALUES = {
            WHITE,
            BLUE,
            GREEN,
            RED,
            BLACK,
            GOLD
    };

    private final String name;

    private GemColor(String name) {
        this.name = name;
    }

    /**
     * Indicates whether this color is the wildcard token.
     *
     * @return true when the color is gold
     */
    public boolean isWildCard() {
        return this == GOLD;
    }

    /**
     * Returns the declared name of this color.
     *
     * @return constant name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the colors in their declared order.
     *
     * @return copy of the color list
     */
    public static GemColor[] values() {
        return VALUES.clone();
    }

    @Override
    public String toString() {
        return name;
    }

    private Object readResolve() throws ObjectStreamException {
        for (GemColor color : VALUES) {
            if (color.name.equals(name)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown GemColor: " + name);
    }
}
