package model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the token inventory available in the shared bank.
 */
public class GemBank implements Serializable {
    private final Map<GemColor, Integer> inventory;

    /**
     * Creates an empty bank with zero tokens recorded for every color.
     */
    public GemBank() {
        this.inventory = new HashMap<>();
        for (GemColor color : GemColor.values()) {
            inventory.put(color, 0);
        }
    }

    /**
     * Adds tokens of one color to the bank.
     *
     * @param color token color to increase
     * @param amount number of tokens to add
     */
    public void addGems(GemColor color, int amount) {
        int current = inventory.get(color);
        inventory.put(color, current + amount);
    }

    /**
     * Removes tokens of one color when enough are available.
     *
     * @param color token color to decrease
     * @param amount number of tokens to remove
     * @return true if the tokens were removed, otherwise false
     */
    public boolean removeGems(GemColor color, int amount) {
        int current = inventory.get(color);
        if (current >= amount) {
            inventory.put(color, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Returns the number of tokens currently stored for one color.
     *
     * @param color token color to query
     * @return current token count
     */
    public int getCount(GemColor color) {
        return inventory.getOrDefault(color, 0);
    }

    /**
     * Returns the number of tokens currently stored for one color.
     *
     * This compatibility alias matches code that uses token terminology.
     *
     * @param color token color to query
     * @return current token count
     */
    public int getTokenCount(GemColor color) {
        return getCount(color);
    }

    /**
     * Adds a batch of token counts to the bank.
     *
     * @param delta token adjustments keyed by color
     */
    public void addTokens(Map<GemColor, Integer> delta) {
        if (delta == null) {
            return;
        }
        for (Map.Entry<GemColor, Integer> entry : delta.entrySet()) {
            addGems(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes a batch of token counts if every requested color is
     * available.
     *
     * @param delta token adjustments keyed by color
     * @return true if all requested tokens were removed, otherwise false
     */
    public boolean removeTokens(Map<GemColor, Integer> delta) {
        if (delta == null || delta.isEmpty()) {
            return true;
        }

        for (Map.Entry<GemColor, Integer> entry : delta.entrySet()) {
            if (getCount(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }

        for (Map.Entry<GemColor, Integer> entry : delta.entrySet()) {
            removeGems(entry.getKey(), entry.getValue());
        }
        return true;
    }

    /**
     * Returns the total number of tokens currently held in the bank.
     *
     * @return total token count across every color
     */
    public int getTotalGemCount() {
        return inventory.values().stream().mapToInt(Integer::intValue).sum();
    }
}
