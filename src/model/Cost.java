package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a token cost keyed by color.
 */
public class Cost implements Serializable {

    private final EnumMap<GemColor, Integer> amounts;

    /**
     * Creates an empty cost.
     */
    public Cost() {
        this.amounts = new EnumMap<>(GemColor.class);
    }

    /**
     * Sets the required amount for a given gem color.
     *
     * @param color token color to update
     * @param amount number of required tokens for that color
     * @throws IllegalArgumentException if the amount is negative or if a non-zero gold cost is supplied
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
     *
     * @param color token color to query
     * @return required token count, or {@code 0} if the color is absent
     */
    public int get(GemColor color) {
        return amounts.getOrDefault(color, 0);
    }

    /**
     * Returns an unmodifiable view of the recorded costs.
     *
     * @return the cost values keyed by color
     */
    public Map<GemColor, Integer> asMap() {
        return Collections.unmodifiableMap(amounts);
    }

    /**
     * Returns the total number of tokens required across all colors.
     *
     * @return the total token cost
     */
    public int total() {
        int sum = 0;
        for (int value : amounts.values()) {
            sum += value;
        }
        return sum;
    }

    /**
     * Indicates whether the cost has no required tokens.
     *
     * @return {@code true} when no costs are recorded
     */
    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    /**
     * Returns a textual representation of the stored costs.
     *
     * @return string form of this cost
     */
    @Override
    public String toString() {
        return amounts.toString();
    }

    /**
     * Compares this cost with another object for value equality.
     *
     * @param obj object to compare against
     * @return {@code true} when both costs contain the same color amounts
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Cost other)) return false;
        return amounts.equals(other.amounts);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return hash code for this cost
     */
    @Override
    public int hashCode() {
        return amounts.hashCode();
    }
}
