package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a token cost based on color.
 */
public class Cost implements Serializable {

    private final EnumMap<GemColor, Integer> amounts;

    /**
     * Creates an empty cost object.
     */
    public Cost() {
        this.amounts = new EnumMap<>(GemColor.class);
    }

    /**
     * Sets the required amount of tokens for a gem color.
     *
     * @param color token color
     * @param amount number of required tokens for that color
     * @throws IllegalArgumentException if the amount is negative or if a non-zero cost is assigned to GOLD color (wild card)
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
     * Returns the required amount of tokens for a gem color.
     *
     * @param color token color 
     * @return required token count, or 0 if the color is absent
     */
    public int get(GemColor color) {
        return amounts.getOrDefault(color, 0);
    }

    /**
     * Returns an immutable overview of the recorded required tokens for each token color 
     *
     * @return the cost values based on color
     */
    public Map<GemColor, Integer> asMap() {
        return Collections.unmodifiableMap(amounts);
    }

    /**
     * Returns the total number of tokens required across all colors.
     *
     * @return total number of tokens cost
     */
    public int total() {
        int sum = 0;
        for (int value : amounts.values()) {
            sum += value;
        }
        return sum;
    }

    /**
     * Indicates whether the cost has no required tokens (if player has enough bonus color points to fulfill cost).
     *
     * @return true when no costs are recorded
     */
    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    /**
     * Returns a string the stored costs.
     *
     */
    @Override
    public String toString() {
        return amounts.toString();
    }

    /**
     * Compares this cost with another object cost
     *
     * @param obj object to compare with
     * @return true when both costs contain the same token color amounts
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Cost other)) return false;
        return amounts.equals(other.amounts);
    }

    /**
     * Returns a hash code that is in line with .equals method above.
     *
     * @return hash code for this cost
     */
    @Override
    public int hashCode() {
        return amounts.hashCode();
    }
}
