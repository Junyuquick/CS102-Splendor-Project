package model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class DevelopmentCard {

    private final int level;
    private final int prestigePoints;
    private final GemColor bonusColor;
    private final Cost cost;

    public DevelopmentCard(int level,
                           int prestigePoints,
                           GemColor bonusColor,
                           Cost cost) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Card level must be 1, 2, or 3");
        }
        this.level = level;

        this.cost = Objects.requireNonNull(cost, "Cost cannot be null");

        if (prestigePoints < 0) {
            throw new IllegalArgumentException("Prestige points cannot be negative");
        }

        if (bonusColor == null || bonusColor == GemColor.GOLD) {
            throw new IllegalArgumentException("Invalid bonus color");
        }

        this.prestigePoints = prestigePoints;
        this.bonusColor = bonusColor;
    }

    public int getLevel() {
        return level;
    }

    public int getPrestigePoints() {
        return prestigePoints;
    }

    public GemColor getBonusColor() {
        return bonusColor;
    }

    public Map<GemColor, Integer> getCost() {
        return Collections.unmodifiableMap(cost.asMap());
    }

    @Override
    public String toString() {
        return "Level: " + level +
               ", Points: " + prestigePoints +
               ", Bonus: " + bonusColor +
               ", Cost: " + cost;
    }
}
