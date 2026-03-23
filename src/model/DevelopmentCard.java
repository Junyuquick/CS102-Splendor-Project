package model;
import java.io.Serializable;import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class DevelopmentCard implements Serializable {

    private final int id;
    private final int level;
    private final int prestigePoints;
    private final GemColor bonusColor;
    private final Cost cost;

    public DevelopmentCard(int id,
                           int level,
                           int prestigePoints,
                           GemColor bonusColor,
                           Cost cost) {
        this.id = id;
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

    public int getId() {
        return id;
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
        return "ID: " + id +
               ", Level: " + level +
               ", Points: " + prestigePoints +
               ", Bonus: " + bonusColor +
               ", Cost: " + cost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DevelopmentCard other)) return false;
        return id == other.id
                && level == other.level
                && prestigePoints == other.prestigePoints
                && bonusColor == other.bonusColor
                && Objects.equals(cost, other.cost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, level, prestigePoints, bonusColor, cost);
    }
}
