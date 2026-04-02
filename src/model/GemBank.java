package model;

import java.io.Serializable;
import java.util.*;

/**
 * Tracks the inventory of tokens in the shared gem bank.
 */
public class GemBank implements Serializable {
    private final Map<GemColor, Integer> inventory;

    /**
     * Creates an empty bank with zero tokens every gem color.
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
     */
    public void addGems(GemColor color, int amount) {
        int current = inventory.get(color);
        inventory.put(color, current + amount);
    }

    /**
     * Removes tokens of one color when gem bank has enough gems to fulfill indicated amount
     *
     * @param color token color to decrease the amount from
     * @param amount number of tokens to remove
     * @return true if tokens withdrawn successfullly, false otherwise
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
     * Returns the amount of gems stored in the gem bank for that color
     *
     * @param color token color
     */
    public int getCount(GemColor color) {
        return inventory.getOrDefault(color, 0);
    }

    /**
     * Returns the number of tokens in the bank for that color.
     *
     * @param color token color 
     */
    public int getTokenCount(GemColor color) {
        return getCount(color);
    }

    /**
     * Adds one/many tokens of different colors to the bank (when a player has used the tokens to purchase a card)
     *
     * @param delta tokens and their respective quantity
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
     * Removes one/many tokens of different colors from bank
     * (Opposite of above method)
     *
     * @param delta tokens and their respective quantities
     * Returns true if all tokens specified are removed, false otherwise
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
     * Returns the total number of tokens in the bank.
     *
     */
    public int getTotalGemCount() {
        return inventory.values().stream()
        .mapToInt(Integer::intValue)
        .sum();
    }
    
}
