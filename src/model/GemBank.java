package model;

import java.io.Serializable;
import java.util.*;

public class GemBank implements Serializable {
    // Maps each color to the number of gems currently held
    private final Map<GemColor, Integer> inventory;

    public GemBank() {
        this.inventory = new HashMap<>();
        // Initialize all colors to zero
        for (GemColor color : GemColor.values()) {
            inventory.put(color, 0);
        }
    }

    // Add gems
    public void addGems(GemColor color, int amount) {
        int current = inventory.get(color);
        inventory.put(color, current + amount);
    }

    //Remove gems from bank
    public boolean removeGems(GemColor color, int amount) {
        int current = inventory.get(color);
        if (current >= amount) {
            inventory.put(color, current - amount);
            return true;
        }
        return false;
    }

    //Get count for colour
    public int getCount(GemColor color) {
        return inventory.getOrDefault(color, 0);
    }

    // Compatibility method used by engine classes
    public int getTokenCount(GemColor color) {
        return getCount(color);
    }

    // Compatibility method used by engine classes
    public void addTokens(Map<GemColor, Integer> delta) {
        if (delta == null) {
            return;
        }
        for (Map.Entry<GemColor, Integer> entry : delta.entrySet()) {
            addGems(entry.getKey(), entry.getValue());
        }
    }

    // Compatibility method used by engine classes
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
     
    public int getTotalGemCount() {
        return inventory.values().stream().mapToInt(Integer::intValue).sum();
    }
    
}
