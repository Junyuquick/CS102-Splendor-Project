//based on assumed code from GemColor Class

package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class Cost implements Serializable {

    private final EnumMap<GemColor, Integer> amounts;

    public Cost() {
        this.amounts = new EnumMap<>(GemColor.class);
    }

    /**
     * Sets the required amount for a given gem color.
     * Amount must be non-negative.
     * GOLD is not allowed in development card costs.
     */
    public void set(GemColor color, int amount) {
        Objects.requireNonNull(color, "GemColor cannot be null");

        if (amount < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }

        if (color == GemColor.GOLD && amount != 0) {
            throw new IllegalArgumentException("GOLD is not allowed in card cost");
        }

        if (amount == 0) {
            amounts.remove(color);
        } else {
            amounts.put(color, amount);
        }
    }

    /**
     * Returns the required amount for a gem color.
     * If not present, returns 0.
     */
    public int get(GemColor color) {
        return amounts.getOrDefault(color, 0);
    }

    /**
     * Returns an unmodifiable view of the cost map.
     */
    public Map<GemColor, Integer> asMap() {
        return Collections.unmodifiableMap(amounts);
    }

    /**
     * Returns the total number of tokens required.
     */
    public int total() {
        int sum = 0;
        for (int value : amounts.values()) {
            sum += value;
        }
        return sum;
    }

    /**
     * Returns true if the cost has no required tokens.
     */
    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    @Override
    public String toString() {
        return amounts.toString();
    }
}