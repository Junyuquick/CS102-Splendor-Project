package model;

import java.util.*;

public class GemBank {
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
     
    public int getTotalGemCount() {
        return inventory.values().stream().mapToInt(Integer::intValue).sum();
    }
    
}
